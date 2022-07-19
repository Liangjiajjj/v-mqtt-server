package com.iot.mqtt.subscribe.manager;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.*;

import java.util.Collection;
import java.util.List;

/**
 * @author liangjiajun
 */
public interface ISubscribeManager {

    /**
     * 存储订阅
     */
    void add(Subscribe subscribe);

    /**
     * 删除订阅
     */
    void remove(Subscribe subscribe);

    /**
     * 删除clientId的订阅
     */
    void removeForClient(String clientId);

    /**
     * 获取订阅存储集
     */
    Collection<Subscribe> search(String topic);

    /**
     * @param clientChannel
     * @param message
     */
    void publishSubscribes(ClientChannel clientChannel, MqttPublishMessage message);

    /**
     * @param clientId
     * @param message
     */
    default void addSubscriptions(String clientId, Long md5Key, MqttSubscribeMessage message) {
        for (MqttTopicSubscription subscription : message.payload().topicSubscriptions()) {
            MqttQoS mqttQoS = subscription.qualityOfService();
            String topicName = subscription.topicName();
            add(Subscribe.builder().clientId(clientId).md5Key(md5Key).topicFilter(topicName).mqttQoS(mqttQoS.value()).build());
        }
    }

    /**
     * @param subscribes
     */
    default void addSubscriptions(List<Subscribe> subscribes) {
        for (Subscribe subscribe : subscribes) {
            add(subscribe);
        }
    }

    /**
     * @param clientId
     * @param message
     */
    default void removeSubscriptions(String clientId, MqttUnsubscribeMessage message) {
        for (String topicName : message.payload().topics()) {
            remove(Subscribe.builder().clientId(clientId).topicFilter(topicName).build());
        }
    }

    /**
     * @param subscribes
     */
    default void removeSubscriptions(List<Subscribe> subscribes) {
        for (Subscribe subscribe : subscribes) {
            remove(subscribe);
        }
    }
}