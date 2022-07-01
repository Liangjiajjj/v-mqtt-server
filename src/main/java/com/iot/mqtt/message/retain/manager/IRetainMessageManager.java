package com.iot.mqtt.message.retain.manager;

import io.vertx.mqtt.messages.MqttPublishMessage;

import java.util.List;

/**
 * @author liangjiajun
 */
public interface IRetainMessageManager {
    /**
     * 存储retain标志消息
     */
    void put(String topicFilter, MqttPublishMessage retainMessageStore);

    /**
     * 获取retain消息
     */
    MqttPublishMessage get(String topicFilter);

    /**
     * 删除retain标志消息
     */
    void remove(String topicFilter);

    /**
     * 判断指定topic的retain消息是否存在
     */
    boolean containsKey(String topicFilter);

    /**
     * 获取retain消息集合
     */
    List<MqttPublishMessage> search(String topicFilter);

}
