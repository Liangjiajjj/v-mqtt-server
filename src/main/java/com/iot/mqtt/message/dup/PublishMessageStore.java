package com.iot.mqtt.message.dup;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
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
        byte[] messageBytes = new byte[message.payload().readableBytes()];
        message.payload().getBytes(message.payload().readerIndex(), messageBytes);
        return PublishMessageStore.builder().messageId(message.variableHeader().messageId())
                .topic(message.variableHeader().topicName()).mqttQoS(message.fixedHeader().qosLevel().value())
                .messageBytes(messageBytes).build();
    }

    public static PublishMessageStore fromMessage(String clientId, MqttPublishMessage message) {
        byte[] messageBytes = new byte[message.payload().readableBytes()];
        message.payload().getBytes(message.payload().readerIndex(), messageBytes);
        return PublishMessageStore.builder().clientId(clientId).messageId(message.variableHeader().messageId())
                .topic(message.variableHeader().topicName()).mqttQoS(message.fixedHeader().qosLevel().value())
                .messageBytes(messageBytes).build();
    }

    public MqttPublishMessage toMessage() {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(mqttQoS), false, 0);
        MqttPublishVariableHeader variableHeader =
                new MqttPublishVariableHeader(topic, messageId, MqttProperties.NO_PROPERTIES);
        ByteBuf buf = Unpooled.copiedBuffer(messageBytes);
        return (MqttPublishMessage) MqttMessageFactory.newMessage(fixedHeader, variableHeader, buf);
    }
}
