package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author liangjiajun
 * 至少一次
 */
@Slf4j
@Service(value = CommonConstant.AT_LEAST_ONCE + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE)
public class AtLeastOnceQosLevelMessageService extends BaseQosLevelMessageService {

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public Future<Integer> publish(ClientChannel fromChannel, Subscribe subscribe, MqttPublishMessage message) {
        String clientId = subscribe.getClientId();
        log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
        Future<Integer> future = publish0(clientId, message);
        // qos = 1/2 需要保存消息，确保发送到位
        // qos 1 收到 PubAck 证明已经完成这次发送 publish 发送
        dupPublishMessageManager.put(clientId, message);
        return future;
    }

    @Override
    public void publishReply(ClientChannel channel, MqttPublishMessage message) {
        channel.publishAcknowledge(message.messageId());
    }

}
