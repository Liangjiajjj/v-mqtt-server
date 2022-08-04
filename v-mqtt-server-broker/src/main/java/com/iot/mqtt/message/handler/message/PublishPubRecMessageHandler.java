package com.iot.mqtt.message.handler.message;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.dup.DupPubRelMessage;
import com.iot.mqtt.message.handler.message.base.BaseMessageHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备信息应答包
 *
 * @author liangjiajun
 */
@Slf4j
@Service(value = "PUBREC" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class PublishPubRecMessageHandler extends BaseMessageHandler<MqttMessage> {

    @Override
    public void handle0(ClientChannel clientChannel, MqttMessage message) {
        String clientId = clientChannel.clientIdentifier();
        MqttPubReplyMessageVariableHeader pubrelVariableHeader = (MqttPubReplyMessageVariableHeader) message.variableHeader();
        int messageId = pubrelVariableHeader.messageId();
        log.debug("PUBREC - clientId: {}, messageId: {}", clientId, messageId);
        // 收到应答包，删除重试消息
        dupPublishMessageManager.remove(clientId, messageId);
        // 加入重新确认列表
        dupPubRelMessageManager.put(clientId, DupPubRelMessage.builder()
                .messageId(messageId)
                .clientId(clientId).build());
        // PUCREC
        clientChannel.publishReceived(messageId);
    }
}

