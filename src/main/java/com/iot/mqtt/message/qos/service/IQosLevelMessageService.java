package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * @author liangjiajun
 */
public interface IQosLevelMessageService {

    /**
     * @param channel   发送着 channel
     * @param subscribe topic 订阅信息
     * @param message
     * @return
     */
    void publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message);

    /**
     * 发送保留消息
     * @param channel
     * @param topicName
     * @param mqttQoS
     */
    void sendRetainMessage(ClientChannel channel, String topicName, MqttQoS mqttQoS);

    /**
     * 回复
     * @param channel
     * @param message
     */
    default void publishReply(ClientChannel channel, MqttPublishMessage message) {

    }

}
