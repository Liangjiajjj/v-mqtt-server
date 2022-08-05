package com.iot.mqtt.pressure.configuration;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.iot.mqtt.pressure.connection.ClientConnection;
import com.iot.mqtt.pressure.connection.ClientConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MqttPressureConfiguration {

    @Value("${pressure.client_count}")
    private Integer clientCount;
    @Resource
    private ClientConnectionPool connectionPool;

    /**
     * 已经失败的客户端id
     */
    private final Set<Integer> failClientIds = new ConcurrentHashSet<>();

    @PostConstruct
    private void init() {
        initClients();
        checkFailClients();
    }

    private void checkFailClients() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (Integer clientId : failClientIds) {
                try {
                    log.info("reconnection clientId {}", clientId);
                    createConnection(clientId);
                } catch (Exception e) {
                    log.error("reconnection error clientId {}", clientId);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void initClients() {
        for (int clientId = 0; clientId < clientCount; clientId++) {
            createConnection(clientId);
        }
    }

    private void createConnection(int id) {
        CompletableFuture<ClientConnection> connection = connectionPool.getConnection(id, new InetSocketAddress("127.0.0.1", 8000));
        connection.whenComplete((clientConnection, throwable) -> {
            clientConnection.connectionFuture().whenComplete((unused, connectionThrowable) -> {
                if (Objects.nonNull(connectionThrowable)) {
                    String clientId = clientConnection.getClientId();
                    failClientIds.add(Integer.parseInt(clientId));
                    return;
                }
                String clientId = clientConnection.getChannel().clientIdentifier();
                clientConnection.subscribe("test");
                if (clientId.equals("0")) {
                    clientConnection.publish("test");
                }
            });
        });
    }
}
