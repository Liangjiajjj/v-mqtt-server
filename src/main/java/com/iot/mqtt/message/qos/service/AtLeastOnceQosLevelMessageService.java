package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author liangjiajun
 * 至少一次
 */
@Slf4j
@Service(value = "AT_LEAST_ONCE")
public class AtLeastOnceQosLevelMessageService implements IQosLevelMessageService {

    @Autowired
    private IMessageIdService messageIdService;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public Future<Integer> publish(ClientSession sendSession, Subscribe subscribe, MqttPublishMessage message) {
        ClientSession toClientSession = clientSessionManager.get(subscribe.getClientId());
        if (Objects.nonNull(toClientSession)) {
            log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
            Future<Integer> future = toClientSession.getEndpoint()
                    .publish(message.topicName(), message.payload(), message.qosLevel(),
                            false, false, messageIdService.getNextMessageId());
            // qos = 1/2 需要保存消息，确保发送到位
            // qos 1 收到 PubAck 证明已经完成这次发送 publish 发送
            dupPublishMessageManager.put(toClientSession.getClientId(), message);
            return future;
        } else {
            return Future.failedFuture(new NullPointerException("toClientSession is null toClientId : " + subscribe.getClientId()));
        }
    }

    @Override
    public void publishReply(ClientSession sendSession, MqttPublishMessage message) {
        sendSession.getEndpoint().publishAcknowledge(message.messageId());
    }

}
