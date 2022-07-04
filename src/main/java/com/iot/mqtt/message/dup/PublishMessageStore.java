package com.iot.mqtt.message.dup;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liangjiajun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishMessageStore {

    private String clientId;

    private String topic;

    private int mqttQoS;

    private int messageId;

    private byte[] messageBytes;

    public static PublishMessageStore fromMessage(MqttPublishMessage message) {
        return PublishMessageStore.builder().messageId(message.messageId())
                .topic(message.topicName()).mqttQoS(message.qosLevel().value())
                .messageBytes(message.payload().getBytes()).build();
    }

    public static PublishMessageStore fromMessage(String clientId, MqttPublishMessage message) {
        return PublishMessageStore.builder().clientId(clientId).messageId(message.messageId())
                .topic(message.topicName()).mqttQoS(message.qosLevel().value())
                .messageBytes(message.payload().getBytes()).build();
    }

    public MqttPublishMessage toMessage() {
        return MqttPublishMessage.create(messageId, MqttQoS.valueOf(mqttQoS), false, false, topic, Unpooled.copiedBuffer(messageBytes));
    }
}
