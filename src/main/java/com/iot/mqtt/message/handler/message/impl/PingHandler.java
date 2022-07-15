package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Service(value = "PINGREQ" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class PingHandler extends BaseMessageHandler<MqttMessage> {

    @Override
    public void handle0(ClientChannel clientChannel, MqttMessage message) {
        String clientId = clientChannel.clientIdentifier();
        ClientSession clientSession = clientSessionManager.get(clientId);
        if (Objects.nonNull(clientSession)) {
            clientSessionManager.expire(clientId, clientSession.getExpire());
        }
    }
}
