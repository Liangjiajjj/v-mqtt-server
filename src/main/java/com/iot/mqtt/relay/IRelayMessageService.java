package com.iot.mqtt.relay;

import io.vertx.mqtt.messages.MqttPublishMessage;

public interface IRelayMessageService {
    /**
     * 转发消息
     * @param brokerId
     * @param clientId
     * @param messageId
     * @param message
     */
    void relayMessage(String brokerId,String clientId, int messageId, MqttPublishMessage message);
}
