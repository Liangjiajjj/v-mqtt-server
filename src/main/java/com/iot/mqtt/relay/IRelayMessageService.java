package com.iot.mqtt.relay;


import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @author liangjiajun
 */
public interface IRelayMessageService {
    /**
     * 转发消息
     *
     * @param clientSession
     * @param messageId
     * @param message
     */
    void relayMessage(ClientSession clientSession, int messageId, MqttPublishMessage message);

    /**
     * 批量转发消息
     *
     * @param clientSession
     * @param publishMessage
     */
    void batchPublish(ClientSession clientSession, PublishMessageStore publishMessage);

    /**
     * 批量转发消息
     *
     */
    void batchPublish0();

}
