package com.iot.mqtt.relay.api;


import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.concurrent.CompletableFuture;

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
    void relayMessage(ClientSession clientSession, int messageId, MqttPublishMessage message, CompletableFuture<Void> future);

    /**
     * 批量转发消息
     *
     * @param clientSession
     * @param publishMessage
     */
    default void batchPublish(ClientSession clientSession, PublishMessageStore publishMessage) {
    }

    /**
     * 批量转发消息
     */
    default void batchPublish0() {
    }

}
