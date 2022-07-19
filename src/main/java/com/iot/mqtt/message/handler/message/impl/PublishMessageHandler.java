package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import com.iot.mqtt.session.ClientSession;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备信息包
 *
 * @author liangjiajun
 */
@Slf4j
@Service(value = "PUBLISH" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class PublishMessageHandler extends BaseMessageHandler<MqttPublishMessage> {

    @Override
    public void handle0(ClientChannel clientChannel, MqttPublishMessage message) {
        String clientId = clientChannel.clientIdentifier();
        String topicName = message.variableHeader().topicName();
        MqttQoS mqttQoS = message.fixedHeader().qosLevel();
        if (log.isTraceEnabled()) {
            log.trace("received clientId : {} topic : {} qoS : {} message size: {}", clientId, topicName, mqttQoS, message.payload().readerIndex());
        }
        // publish 延长session失效时间
        if (clientSessionManager.containsKey(clientId)) {
            ClientSession clientSession = clientSessionManager.get(clientId);
            if (clientSession.getBrokerId().equals(mqttConfig.getBrokerId())) {
                clientSessionManager.expire(clientId, clientSession.getExpire());
            }
        }
        // 发送到订阅消息的客户端
        subscribeManager.publishSubscribes(clientChannel, message);
        // 返回客户端
        IQosLevelMessageService qosLevelMessageService = getQosLevelMessageService(mqttQoS);
        qosLevelMessageService.publishReply(clientChannel, message);
    }

}