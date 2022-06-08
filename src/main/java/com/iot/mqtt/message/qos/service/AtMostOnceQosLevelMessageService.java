package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service(value = "AT_MOST_ONCE")
public class AtMostOnceQosLevelMessageService implements IQosLevelMessageService {

    @Autowired
    private IMessageIdService messageIdService;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Override
    public Future<Integer> publish(ClientSession sendSession, Subscribe subscribe, MqttPublishMessage message) {
        ClientSession toClientSession = clientSessionManager.get(subscribe.getClientId());
        if (Objects.nonNull(toClientSession)) {
            log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
            return toClientSession.getEndpoint()
                    .publish(message.topicName(), message.payload(), message.qosLevel(),
                            false, false, messageIdService.getNextMessageId());
        } else {
            return Future.failedFuture(new NullPointerException("toClientSession is null toClientId : " + subscribe.getClientId()));
        }
    }

    @Override
    public void publishReply(ClientSession sendSession, MqttPublishMessage message) {

    }
}
