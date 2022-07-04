package com.iot.mqtt.message.dup.manager.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "true")
public class RedisDupPublishMessageManager implements IDupPublishMessageManager {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void put(String clientId, MqttPublishMessage publishMessage) {
        int messageId = publishMessage.messageId();
        getRMap(clientId).put(String.valueOf(messageId), PublishMessageStore.fromMessage(clientId, publishMessage));
    }

    @Override
    public Collection<MqttPublishMessage> get(String clientId) {
        return getRMap(clientId).values().stream().map(PublishMessageStore::toMessage).collect(Collectors.toList());
    }

    @Override
    public void remove(String clientId, int messageId) {
        getRMap(clientId).remove(String.valueOf(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        getRMap(clientId).delete();
    }

    private RMap<String, PublishMessageStore> getRMap(String clientId) {
        return redissonClient.getMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId));
    }
}
