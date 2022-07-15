package com.iot.mqtt.message.handler.base;

import com.iot.mqtt.context.MqttServiceContext;
import com.iot.mqtt.redis.annotation.RedisBatch;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
public abstract class BaseMessageHandler<E extends MqttMessage> implements IHandler<E> {

    @Autowired
    protected MqttConfig mqttConfig;
    @Autowired
    protected MqttServiceContext context;
    @Autowired
    protected IClientSessionManager clientSessionManager;
    @Autowired
    protected IClientChannelManager clientChannelManager;
    @Autowired
    protected ISubscribeManager subscribeManager;
    @Autowired
    protected IDupPubRelMessageManager dupPubRelMessageManager;
    @Autowired
    protected IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public void handle(Channel channel, E e) {
        String clientId = getClientId(channel);
        if (log.isTraceEnabled()) {
            log.trace("MessageHandler handle clientId {} , threadName {} ", clientId, Thread.currentThread().getName());
        }
        ClientChannel clientChannel = clientChannelManager.get(clientId);
        if (Objects.isNull(clientChannel)) {
            log.warn("MessageHandler handle clientChannel is null {} ", clientId);
            return;
        }
        clientChannel.getExecutor().execute(() -> {
            try {
                this.handle0(clientChannel, e);
            } catch (Throwable throwable) {
                log.error("MessageHandler handle0 error channelId {} , messageId {} ", channel.id().asLongText(), e.fixedHeader().messageType(), throwable);
            } finally {
                ReferenceCountUtil.release(e);
            }
        });
    }

    /**
     * 处理业务逻辑
     *
     * @param e
     */
    public void handle0(ClientChannel clientChannel, E e) {
    }

    /**
     * 根据不同的MqttQoS，有不同的发送策略
     *
     * @param mqttQoS
     * @return
     */
    protected IQosLevelMessageService getQosLevelMessageService(MqttQoS mqttQoS) {
        return context.getQosLevelMessageService(mqttQoS);
    }

    /**
     * 清空信息
     *
     * @param clientId
     */
    public void removeForClient(String clientId) {
        subscribeManager.removeForClient(clientId);
        dupPubRelMessageManager.removeByClient(clientId);
        dupPublishMessageManager.removeByClient(clientId);
    }
}
