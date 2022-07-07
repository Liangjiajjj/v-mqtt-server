package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author liangjiajun
 */
@Slf4j
@Service(value = CommonConstant.AT_MOST_ONCE + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE)
public class AtMostOnceQosLevelMessageService extends BaseQosLevelMessageService {

    @Override
    public void publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message) {
        String toClientId = subscribe.getClientId();
        log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
        publish0(toClientId, message);
    }

    @Override
    public void publishReply(ClientChannel channel, MqttPublishMessage message) {

    }
}
