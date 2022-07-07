package com.iot.mqtt.message.handler.message;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 发送消息的应答包
 *
 * @author liangjiajun
 */
@Service(value = "PUBACK" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class PubAckMessageHandler extends BaseMessageHandler<MqttPubAckMessage> {

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public void handle0(ClientChannel clientChannel, MqttPubAckMessage message) {
        String clientId = clientChannel.clientIdentifier();
        // 收到应答包，删除重试消息
        dupPublishMessageManager.remove(clientId, message.variableHeader().messageId());
    }
}
