package com.iot.mqtt.dup;

import com.iot.mqtt.filter.BaseTopicBean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishMessageStore extends BaseTopicBean {

    private String clientId;

    private String brokerId;

    private String topic;

    private int mqttQoS;

    private int messageId;

    private byte[] messageBytes;

    public static PublishMessageStore fromMessage(String topic) {
        return PublishMessageStore.builder().topic(topic).build();
    }

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

    public static PublishMessageStore fromMessage(String brokerId, String clientId, MqttPublishMessage message) {
        byte[] messageBytes = new byte[message.payload().readableBytes()];
        message.payload().getBytes(message.payload().readerIndex(), messageBytes);
        return PublishMessageStore.builder().brokerId(brokerId).clientId(clientId).messageId(message.variableHeader().messageId())
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

    @Override
    public String getTopicFilter() {
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublishMessageStore that = (PublishMessageStore) o;
        return Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

}
