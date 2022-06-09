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
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "false")
public class CacheClientSessionManager implements IClientSessionManager {

    /**
     * Session 列表
     */
    private final static Map<String, ClientSession> CLIENT_SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public ClientSession register(String brokerId, MqttEndpoint endpoint, EventExecutor executor) {
        ClientSession clientSession = new ClientSession(brokerId, endpoint, executor);
        CLIENT_SESSION_MAP.put(endpoint.clientIdentifier(), clientSession);
        return clientSession;
    }

    @Override
    public void expire(String clientId, int expire) {

    }

    @Override
    public ClientSession get(String clientId) {
        return CLIENT_SESSION_MAP.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return CLIENT_SESSION_MAP.containsKey(clientId);
    }

    @Override
    public void remove(String clientId) {
        CLIENT_SESSION_MAP.remove(clientId);
    }
}
