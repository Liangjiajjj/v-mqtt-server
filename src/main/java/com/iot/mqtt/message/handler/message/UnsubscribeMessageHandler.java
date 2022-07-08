package com.iot.mqtt.message.handler.message;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.topic.Subscribe;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 退订topic
 *
 * @author liangjiajun
 */
@Slf4j
@Service(value = "UNSUBSCRIBE" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class UnsubscribeMessageHandler extends BaseMessageHandler<MqttUnsubscribeMessage> {

    @Autowired
    private ISubscribeManager subscribeManager;

    @Override
    public void handle0(ClientChannel clientChannel, MqttUnsubscribeMessage mqttUnsubscribeMessage) {
        String clientId = clientChannel.clientIdentifier();
        for (String topicName : mqttUnsubscribeMessage.payload().topics()) {
            log.debug("Unsubscription ClientId {} for {} ", clientId, topicName);
            subscribeManager.remove(Subscribe.builder().clientId(clientId).topicFilter(topicName).build());
        }
        // 确认订阅请求
        clientChannel.unsubscribeAcknowledge(mqttUnsubscribeMessage.variableHeader().messageId());
    }
}