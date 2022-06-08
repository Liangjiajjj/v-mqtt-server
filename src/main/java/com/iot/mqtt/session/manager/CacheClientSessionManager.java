package com.iot.mqtt.session.manager;

import com.iot.mqtt.session.ClientSession;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.mqtt.MqttEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存session
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "false")
public class CacheClientSessionManager implements IClientSessionManager {

    /**
     * Session 列表
     */
    private final static Map<String, ClientSession> clientSessionMap = new ConcurrentHashMap<>();

    @Override
    public ClientSession register(String brokerId, MqttEndpoint endpoint, EventExecutor executor) {
        ClientSession clientSession = new ClientSession(brokerId, endpoint, executor);
        clientSessionMap.put(endpoint.clientIdentifier(), clientSession);
        return clientSession;
    }

    @Override
    public void expire(String clientId, int expire) {

    }

    @Override
    public ClientSession get(String clientId) {
        return clientSessionMap.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return clientSessionMap.containsKey(clientId);
    }

    @Override
    public void remove(String clientId) {
        clientSessionMap.remove(clientId);
    }
}
