package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;

/**
 * @author liangjiajun
 */
public interface IQosLevelMessageService {

    /**
     * @param channel 发送着 channel
     * @param subscribe   topic 订阅信息
     * @param message
     * @return
     */
    Future<Integer> publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message);

    default void publishReply(ClientChannel channel, MqttPublishMessage message) {

    }
}
