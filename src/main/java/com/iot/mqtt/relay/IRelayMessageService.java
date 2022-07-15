package com.iot.mqtt.relay;


import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @author liangjiajun
 */
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
