package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import com.iot.mqtt.redis.annotation.RedisBatch;
import com.iot.mqtt.relay.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

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

    @Override
    public void sendRetainMessage(ClientChannel clientChannel, String topicName) {
        for (MqttPublishMessage message : retainMessageManager.search(topicName)) {
            publish0(clientChannel.clientIdentifier(), message);
        }
    }

    protected void publish0(String toClientId, MqttPublishMessage message) {
        ClientSession session = clientSessionManager.get(toClientId);
        if (Objects.isNull(session)) {
            log.error("publish0 session toClientId {} is null", toClientId);
            return;
        }
        String brokerId = session.getBrokerId();
        int messageId = messageIdService.getNextMessageId();
        // 不是在本机内的链接，转发
        if (!mqttConfig.getBrokerId().equals(brokerId)) {
            if (log.isTraceEnabled()) {
                log.trace("relay message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
            }
            relayMessageService.relayMessage(brokerId, toClientId, messageId, message);
            return;
        }
        // 延长存活时间
        clientSessionManager.expire(toClientId, session.getExpire());
        ClientChannel channel = clientChannelManager.get(toClientId);
        if (Objects.isNull(channel)) {
            log.error("publish0 channel toClientId {} is null", toClientId);
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("publish message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
        }
        byte[] messageBytes = new byte[message.payload().readableBytes()];
        message.payload().getBytes(message.payload().readerIndex(), messageBytes);
        channel.publish(message.variableHeader().topicName(), messageBytes, message.fixedHeader().qosLevel(), false, false, messageId);
    }

}
