package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;

/**
 * @author liangjiajun
 */
public interface IQosLevelMessageService {

    /**
     * @param sendSession 发送着 Session
     * @param subscribe   topic 订阅信息
     * @param message
     * @return
     */
    Future<Integer> publish(ClientSession sendSession, Subscribe subscribe, MqttPublishMessage message);

    default void publishReply(ClientSession sendSession, MqttPublishMessage message) {

    }
}
