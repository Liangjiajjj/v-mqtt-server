package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
@Service(value = "DISCONNECT" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class DisconnectMessageHandler extends BaseMessageHandler<MqttMessage> {

    @Override
    public void handle0(ClientChannel channel, MqttMessage mqttMessage) {
        String clientId = channel.clientIdentifier();
        ClientSession clientSession = clientSessionManager.get(clientId);
        if (Objects.nonNull(clientSession) && clientSession.getIsCleanSession()) {
            removeForClient(clientId);
        }
        log.debug("DISCONNECT - clientId: {}, cleanSession: {}", clientId, clientSession.getIsCleanSession());
        clientSessionManager.remove(clientId);
        clientChannelManager.remove(clientId);
        channel.close();
    }
}
