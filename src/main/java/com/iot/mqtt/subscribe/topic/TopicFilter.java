package com.iot.mqtt.subscribe.topic;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.Set;

/**
 * @author liangjiajun
 */
public interface TopicFilter {

    /**
     * 获取订阅topic
     *
     * @param topic   topic
     * @return {@link Subscribe}
     */
    Set<Subscribe> getSubscribeByTopic(String topic);


    /**
     * 保存订阅topic
     *
     * @param topicFilter topicFilter
     * @param clientId {@link String}
     * @param mqttQoS     {@link MqttQoS}
     */
    void addSubscribeTopic(String topicFilter, String clientId, MqttQoS mqttQoS);


    /**
     * 保存订阅topic
     *
     * @param Subscribe {@link Subscribe}
     */
    void addSubscribeTopic(Subscribe Subscribe);


    /**
     * 保存订阅topic
     *
     * @param Subscribe {@link Subscribe}
     */
    void removeSubscribeTopic(Subscribe Subscribe);


    /**
     * 获取订阅总数
     *
     * @return 总数
     */
    int count();


    /**
     * 获取订所有订阅topic
     *
     * @return {@link Subscribe}
     */
    Set<Subscribe> getAllSubscribesTopic();


}
