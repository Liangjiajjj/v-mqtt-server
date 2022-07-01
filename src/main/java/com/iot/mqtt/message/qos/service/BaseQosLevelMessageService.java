package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.BrokerConfig;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import com.iot.mqtt.relay.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@Slf4j
public abstract class BaseQosLevelMessageService implements IQosLevelMessageService {

    @Autowired
    private BrokerConfig brokerConfig;

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
    public void sendRetainMessage(ClientChannel channel, String topicName) {
        for (MqttPublishMessage message : retainMessageManager.search(topicName)) {
            publish0(channel.getClientId(), message);
        }
    }

    protected Future<Integer> publish0(String toClientId, MqttPublishMessage message) {
        ClientSession session = clientSessionManager.get(toClientId);
        if (Objects.isNull(session)) {
            return Future.failedFuture("session is null !!!");
        }
        String brokerId = session.getBrokerId();
        int messageId = messageIdService.getNextMessageId();
        // 不是在本机内的链接，转发
        if (!brokerConfig.getBrokerId().equals(brokerId)) {
            if (log.isTraceEnabled()) {
                log.trace("relay message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
            }
            relayMessageService.relayMessage(brokerId, toClientId, messageId, message);
            return Future.failedFuture("relay message brokerId : " + brokerId);
        }
        // 延长存活时间
        clientSessionManager.expire(toClientId, session.getExpire());
        ClientChannel channel = clientChannelManager.get(toClientId);
        if (Objects.isNull(channel)) {
            return Future.failedFuture("channel is null !!!");
        }
        if (log.isTraceEnabled()) {
            log.trace("publish message brokerId:{} , clientId:{} , messageId:{} ", brokerId, toClientId, messageId);
        }
        return channel.publish(message.topicName(), message.payload(), message.qosLevel(), false, false, messageId);
    }

}
