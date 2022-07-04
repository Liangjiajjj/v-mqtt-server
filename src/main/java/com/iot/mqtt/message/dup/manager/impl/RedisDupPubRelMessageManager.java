package com.iot.mqtt.message.dup.manager.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.DupPubRelMessage;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "true")
public class RedisDupPubRelMessageManager implements IDupPubRelMessageManager {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void put(String clientId, DupPubRelMessage publishMessage) {
        int messageId = publishMessage.getMessageId();
        getRMap(clientId).put(String.valueOf(messageId), publishMessage);
    }

    @Override
    public Collection<DupPubRelMessage> get(String clientId) {
        return getRMap(clientId).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        getRMap(clientId).remove(String.valueOf(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        getRMap(clientId).delete();
    }

    private RMap<String, DupPubRelMessage> getRMap(String clientId) {
        return redissonClient.getMap(RedisKeyConstant.DUP_PUBREL_KEY.getKey(clientId));
    }
}
