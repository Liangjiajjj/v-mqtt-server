package com.iot.mqtt.dup.manager.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisDupPublishMessageManagerImpl extends RedisBaseServiceImpl<PublishMessageStore> implements IDupPublishMessageManager, RedisBaseService<PublishMessageStore> {

    @Override
    public void put(String clientId, MqttPublishMessage publishMessage) {
        int messageId = publishMessage.variableHeader().messageId();
        putMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId), String.valueOf(messageId), PublishMessageStore.fromMessage(clientId, publishMessage));
    }

    @Override
    public Collection<MqttPublishMessage> get(String clientId) {
        return getMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId)).values()
                .stream().map(PublishMessageStore::toDirectMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(String clientId, int messageId) {
        removeMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId), String.valueOf(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        removeMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId));
    }

/*    private RMap<String, PublishMessageStore> getRMap(String clientId) {
        return redissonClient.getMap(RedisKeyConstant.DUP_PUBLISH_KEY.getKey(clientId));
    }*/
}
