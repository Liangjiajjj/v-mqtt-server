package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
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
public class UnsubscribeMessageHandler extends BaseMessageHandler<MqttUnsubscribeMessage>  {

    @Autowired
    private ISubscribeManager subscribeManager;

    @Override
    public void handle0(ClientChannel clientChannel, MqttUnsubscribeMessage mqttUnsubscribeMessage) {
        String clientId = clientChannel.clientIdentifier();
        subscribeManager.removeSubscriptions(clientId, mqttUnsubscribeMessage);
        // 确认订阅请求
        clientChannel.unsubscribeAcknowledge(mqttUnsubscribeMessage.variableHeader().messageId());
    }

}