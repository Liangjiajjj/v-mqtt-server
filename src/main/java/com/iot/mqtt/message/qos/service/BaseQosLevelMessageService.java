package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.messageid.service.IMessageIdService;
import com.iot.mqtt.retain.manager.IRetainMessageManager;
import com.iot.mqtt.relay.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import com.iot.mqtt.util.FutureUtil;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class BaseQosLevelMessageService implements IQosLevelMessageService {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private IMessageIdService messageIdService;

    @Autowired
    private IRelayMessageService relayMessageService;

    @Autowired
    private IRetainMessageManager retainMessageManager;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Autowired
    private IClientChannelManager clientChannelManager;

    @Resource(name = "PUBLISH-EXECUTOR")
    private MqttEventExecuteGroup mqttEventExecuteGroup;

    @Override
    public CompletableFuture<Void> sendRetainMessage(ClientChannel clientChannel, String topicName, MqttQoS mqttQoS) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (PublishMessageStore message : retainMessageManager.search(topicName)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);
            mqttEventExecuteGroup.get(clientChannel.getMd5Key()).execute(() -> {
                sendRetainMessage0(clientChannel, mqttQoS, message, future);
            });
        }
        return FutureUtil.waitForAll(futures);
    }

    private void sendRetainMessage0(ClientChannel clientChannel, MqttQoS mqttQoS, PublishMessageStore message, CompletableFuture<Void> future) {
        MqttPublishMessage publishMessage = null;
        try {
            publishMessage = message.toDirectMessage();
            MqttQoS respQoS = message.getMqttQoS() > mqttQoS.value() ? mqttQoS : MqttQoS.valueOf(message.getMqttQoS());
            message.setMqttQoS(respQoS.value());
            publish0(clientChannel.clientIdentifier(), message.toMessage(), future);
        } catch (Throwable throwable) {
            log.error("sendRetainMessage0 error !!! ", throwable);
            future.completeExceptionally(throwable);
        } finally {
            if (Objects.nonNull(publishMessage)) {
                ReferenceCountUtil.release(publishMessage);
            }
        }
    }

    protected void publish0(String toClientId, MqttPublishMessage message, CompletableFuture<Void> future) {
        try {
            if (!clientSessionManager.containsKey(toClientId)) {
                log.error("publish0 session toClientId {} is null", toClientId);
                return;
            }
            ClientSession session = clientSessionManager.get(toClientId);
            String brokerId = session.getBrokerId();
            int messageId = messageIdService.getNextMessageId();
            // 不是在本机内的链接，转发
            if (!mqttConfig.getBrokerId().equals(brokerId)) {
                if (log.isTraceEnabled()) {
                    log.trace("relay message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
                }
                relayMessageService.relayMessage(session, messageId, message, future);
                return;
            }
            ClientChannel channel = clientChannelManager.get(toClientId);
            if (Objects.isNull(channel)) {
                log.error("publish0 channel toClientId {} is null", toClientId);
                future.complete(null);
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("publish message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
            }
            message.payload().retain();
            channel.publish(message.variableHeader().topicName(), message.payload(), message.fixedHeader().qosLevel(), false, false, messageId);
        } catch (Exception e) {
            log.error("publish0 session toClientId {} error !!!! ", toClientId);
            future.completeExceptionally(e);
        }
    }

}
