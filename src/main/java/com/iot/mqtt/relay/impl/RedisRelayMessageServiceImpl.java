package com.iot.mqtt.relay.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.event.SessionRefreshEvent;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.annotation.RedisBatch;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.relay.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.RelayMessageQueue;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import com.iot.mqtt.util.Md5Util;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@Conditional(RedisRelayMessageServiceImpl.RedisRelayProperty.class)
public class RedisRelayMessageServiceImpl extends RedisBaseServiceImpl<PublishMessageStore> implements IRelayMessageService, RedisBaseService<PublishMessageStore> {

    @Resource
    private MqttConfig mqttConfig;

    @Resource(name = "PUBLISH-EXECUTOR")
    private MqttEventExecuteGroup publishExecutor;

    @Resource(name = "RELAY-PUBLISH-CLIENT-EXECUTOR")
    private MqttEventExecuteGroup relayPublishClientExecutor;

    @Resource
    private IClientChannelManager clientChannelManager;

    /**
     * 远程链接,主要是存储批量推送的消息的
     * <p>
     * 批量推送方案：
     * 1.每次新增推送消息时，用clientId取出当前线程的本地内存队列
     * 2.如果到达100条消息进行推送，推送完回收本地内存队列
     * 3.每5秒检查所有线程是否有遗漏消息，如果有在当前线程执行推送
     * 4.当session发生变化时候用clientId取出当前线程执行推送遗漏消息
     */
    private static final ThreadLocal<RelayMessageQueue> RELAY_MESSAGE_QUEUE_LOCAL = new ThreadLocal<>();

    @PostConstruct
    private void init() {
        String selfTopic = RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(mqttConfig.getBrokerId());
        log.info("subscription self topic {} ", selfTopic);
        getTopic(selfTopic).addListener(PublishMessageStore.class, (channel, msg) -> {
            String clientId = msg.getClientId();
            int messageId = msg.getMessageId();
            if (log.isTraceEnabled()) {
                log.trace("receive relay message clientId:{} , messageId:{} , message {}", clientId, messageId, new String(msg.getMessageBytes(), StandardCharsets.UTF_8));
            }
            ClientChannel clientChannel = clientChannelManager.get(clientId);
            if (Objects.isNull(clientChannel)) {
                log.warn("receive relay message, Channel is null ... clientId:{} , messageId:{} ", clientId, msg.getMessageId());
                return;
            }
            MqttPublishMessage publishMessage = msg.toDirectMessage();
            publishExecutor.get(clientChannel.getMd5Key()).execute(() -> {
                try {
                    clientChannel.publish(messageId, publishMessage);
                } catch (Throwable throwable) {
                    log.error("listener relay error message !!!", throwable);
                } finally {
                    ReferenceCountUtil.release(publishMessage);
                }
            });
        });
        // 如果是批量转发，创建定时线程池
        if (mqttConfig.getIsBatchRelay() && mqttConfig.getIsRedisKeyNotify()) {
            initCheckRelayMessageTask();
        }
    }

    @Override
    public void relayMessage(ClientSession clientSession, int messageId, MqttPublishMessage message, CompletableFuture<Void> future) {
        relayPublishClientExecutor.get(clientSession.getMd5Key()).execute(() -> {
            relayMessage0(clientSession, messageId, message);
            future.complete(null);
        });
    }

    @Override
    public void batchPublish(ClientSession clientSession, PublishMessageStore publishMessage) {
        relayPublishClientExecutor.assertEventLoop(clientSession.getMd5Key());
        RelayMessageQueue relayMessageQueue = getLocalRelayMessageQueue();
        relayMessageQueue.add(publishMessage);
        if (relayMessageQueue.isCanPush(mqttConfig.getBatchRelayCount())) {
            // 发送当前线程所有的队列
            log.info("canPush batch publish relay message !!! ");
            batchPublish0();
        }
    }


    /**
     * session变化是应该推送完所有的数据给它
     *
     * @param event
     */
    @EventListener
    public void onSessionRefreshEvent(SessionRefreshEvent event) {
        if (mqttConfig.getIsBatchRelay() && mqttConfig.getIsRedisKeyNotify()) {
            relayPublishClientExecutor.get(Md5Util.hash(event.getClientId())).execute(this::batchPublish0);
        }
    }

    /**
     * 检查是否有遗漏的消息
     */
    private void initCheckRelayMessageTask() {
        for (EventExecutor executor : relayPublishClientExecutor.getEventExecutor()) {
            executor.scheduleAtFixedRate(this::batchPublish0, 0, mqttConfig.getMaxBatchRelayDelay(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 把当前线程的队列进行批量推送
     */
    @Override
    @RedisBatch
    public void batchPublish0() {
        try {
            RelayMessageQueue relayMessageQueue = getLocalRelayMessageQueue();
            if (relayMessageQueue.isEmpty()) {
                return;
            }
            log.info("batch publish relay message count:{} ", relayMessageQueue.getCount());
            for (PublishMessageStore publishMessage : relayMessageQueue.getRelayMessageQueue()) {
                publish(RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(publishMessage.getBrokerId()), publishMessage);
            }
        } catch (Exception e) {
            log.error("batchPublish0 error !!! ", e);
        } finally {
            // 回收内存
            RELAY_MESSAGE_QUEUE_LOCAL.remove();
        }
    }

    /**
     * 转发消息
     *
     * @param clientSession
     * @param messageId
     * @param message
     */
    private void relayMessage0(ClientSession clientSession, int messageId, MqttPublishMessage message) {
        try {
            PublishMessageStore publishMessage = PublishMessageStore.fromMessage(clientSession.getBrokerId(),
                    clientSession.getClientId(), message);
            publishMessage.setMessageId(messageId);
            if (mqttConfig.getIsBatchRelay() && mqttConfig.getIsRedisKeyNotify()) {
                batchPublish(clientSession, publishMessage);
            } else {
                publish(RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(clientSession.getBrokerId()), publishMessage);
            }
        } catch (Throwable throwable) {
            log.error("relayMessage error !!! clientId {} messageId {}", clientSession.getClientId(), messageId, throwable);
        }
    }

    /**
     * 取出当前线程的队列
     *
     * @return
     */
    public static RelayMessageQueue getLocalRelayMessageQueue() {
        RelayMessageQueue relayMessageQueue = RELAY_MESSAGE_QUEUE_LOCAL.get();
        if (Objects.isNull(relayMessageQueue)) {
            relayMessageQueue = new RelayMessageQueue();
            RELAY_MESSAGE_QUEUE_LOCAL.set(relayMessageQueue);
        }
        return relayMessageQueue;
    }

    static class RedisRelayProperty extends AllNestedConditions {

        public RedisRelayProperty() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
        static class ClusterEnabled {
        }

        @ConditionalOnProperty(name = "mqtt.is_open_relay_server", havingValue = "false")
        static class OpenRelayServer {
        }
    }

}
