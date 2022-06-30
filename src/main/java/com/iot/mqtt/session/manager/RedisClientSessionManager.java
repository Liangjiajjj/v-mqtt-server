package com.iot.mqtt.session.manager;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.session.ClientSession;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttEndpoint;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * reids session
 *
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "true")
public class RedisClientSessionManager implements IClientSessionManager {

    @Autowired
    private RedissonClient redissonClient;
    /**
     * Session 列表（内存，本机存储）
     */
    private final static Map<String, ClientSession> CLIENT_SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public ClientSession register(String brokerId, MqttEndpoint endpoint ) {
        String clientId = endpoint.clientIdentifier();
        ClientSession clientSession = ClientSession.builder().brokerId(brokerId)
                .clientId(clientId)
                .isCleanSession(endpoint.isCleanSession())
                .will(endpoint.will()).build();
        CLIENT_SESSION_MAP.put(clientId, clientSession);
        getRedisSessionMap(clientId).set(clientSession.toJson());
        return clientSession;
    }

    @Override
    public ClientSession get(String clientId) {
        return CLIENT_SESSION_MAP.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return getRedisSessionMap(clientId).isExists();
    }

    @Override
    public void remove(String clientId) {
        CLIENT_SESSION_MAP.remove(clientId);
        getRedisSessionMap(clientId).delete();
    }

    @Override
    public void expire(String clientId, int expire) {

    }

    private RBucket<JsonObject> getRedisSessionMap(String clientId) {
        return redissonClient.getBucket(RedisKeyConstant.CLIENT_SESSION.getRedisKey(clientId));
    }
}
