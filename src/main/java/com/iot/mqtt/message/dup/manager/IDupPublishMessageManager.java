package com.iot.mqtt.message.dup.manager;

import io.vertx.mqtt.messages.MqttPublishMessage;

import java.util.Collection;

/**
 * PUBLISH重发消息存储服务接口, 当QoS=1和QoS=2时存在该重发机制
 */
public interface IDupPublishMessageManager {

    /**
     * 存储消息
     */
    void put(String clientId, MqttPublishMessage publishMessage);

    /**
     * 获取消息集合
     */
    Collection<MqttPublishMessage> get(String clientId);

    /**
     * 删除消息
     */
    void remove(String clientId, int messageId);

    /**
     * 删除消息
     */
    void removeByClient(String clientId);

}
