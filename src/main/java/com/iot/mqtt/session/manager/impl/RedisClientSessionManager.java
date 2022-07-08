package com.iot.mqtt.session.manager.impl;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * redis session
 *
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisClientSessionManager implements IClientSessionManager {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public ClientSession register(String brokerId, ClientChannel clientChannel, int expire) {
        String clientId = clientChannel.clientIdentifier();
        ClientSession clientSession = ClientSession.builder().brokerId(brokerId)
                .expire(expire)
                .clientId(clientId)
                .isCleanSession(clientChannel.isCleanSession())
                .will(clientChannel.will()).build();
        getRBucket(clientId).set(clientSession.toJson());
        if (expire > 0) {
            expire(clientId, expire);
        }
        return clientSession;
    }

    @Override
    public ClientSession get(String clientId) {
        return Optional.ofNullable(getRBucket(clientId).get())
                .map((json) -> new ClientSession().fromJson(json))
                .orElse(null);
    }

    @Override
    public boolean containsKey(String clientId) {
        return getRBucket(clientId).isExists();
    }

    @Override
    public void remove(String clientId) {
        getRBucket(clientId).delete();
    }

    @Override
    public void expire(String clientId, int expire) {
        getRBucket(clientId).expire(expire, TimeUnit.SECONDS);
    }

    private RBucket<JSONObject> getRBucket(String clientId) {
        return redissonClient.getBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId));
    }
}
