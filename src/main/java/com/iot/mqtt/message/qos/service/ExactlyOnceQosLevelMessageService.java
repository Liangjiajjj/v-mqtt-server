package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 暂时不支持qos
 * QoS2可在业务层做，比如在payload中增加去重标记,减少发包数量
 * <p>
 * qos2：确保一次送达，我给你发 (PUBLISH)，你给我回一个你收到了 (PUBREC)，我再给你发
 * 一 个你确定你收到了吗 （PUBREL），你再给我回一个收到了别发了求你了 (PUBCOMP)
 */

@Service(value = "EXACTLY_ONCE")
public class ExactlyOnceQosLevelMessageService implements IQosLevelMessageService {

    @Autowired
    private IMessageIdService messageIdService;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public Future<Integer> publish(ClientSession sendSession, Subscribe subscribe, MqttPublishMessage message) {
        String toClientId = subscribe.getClientId();
        ClientSession toClientSession = clientSessionManager.get(toClientId);
        if (Objects.nonNull(toClientSession)) {
            Future<Integer> future = toClientSession.getEndpoint()
                    .publish(message.topicName(), message.payload(), message.qosLevel(),
                            false, false, messageIdService.getNextMessageId());
            // qos = 1/2 需要保存消息，确保发送到位
            dupPublishMessageManager.put(toClientSession.getClientId(), message);
            return future;
        } else {
            return Future.failedFuture(new NullPointerException("toClientSession is null toClientId : " + toClientId));
        }
    }

    @Override
    public void publishReply(ClientSession sendSession, MqttPublishMessage message) {
        sendSession.getEndpoint().publishRelease(message.messageId());
    }
}
