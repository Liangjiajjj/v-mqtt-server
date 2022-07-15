package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author liangjiajun
 */
@Slf4j
@Service(value = "PUBCOMP" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class PublishPubCompMessageHandler extends BaseMessageHandler<MqttMessage> {

    @Override
    public void handle0(ClientChannel clientChannel, MqttMessage message) {
        MqttPubReplyMessageVariableHeader pubcompVariableHeader = (io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader) message.variableHeader();
        String clientId = clientChannel.clientIdentifier();
        int messageId = pubcompVariableHeader.messageId();
        log.debug("PUBCOMP - clientId: {}, messageId: {}", clientId, messageId);
        dupPubRelMessageManager.remove(clientId, messageId);
    }
}
