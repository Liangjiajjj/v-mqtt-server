package com.iot.mqtt.message.api;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.nio.Buffer;
import java.util.concurrent.CompletableFuture;

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
    void publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message, CompletableFuture<Void> future);


    /**
     * 发送保留消息
     *
     * @param channel
     * @param topicName
     * @param mqttQoS
     */
    CompletableFuture<Void> sendRetainMessage(ClientChannel channel, String topicName, MqttQoS mqttQoS);

    /**
     * 回复
     *
     * @param channel
     */
    default void publishReply(ClientChannel channel, Integer messageId) {

    }

}
