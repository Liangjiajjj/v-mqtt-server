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

    @Value("${pressure.mqtt_host}")
    private String mqttHost;
    @Value("${pressure.mqtt_port}")
    private Integer mqttPort;
    @Value("${pressure.start_index}")
    private Integer startIndex;
    @Value("${pressure.client_count}")
    private Integer clientCount;
    @Resource
    private ClientConnectionPool connectionPool;

    @PostConstruct
    private void init() {
        checkFailClients();
        initClients();
    }

    private void checkFailClients() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (Integer clientId : connectionPool.getFailClientIds()) {
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
        for (int clientId = startIndex; clientId < startIndex + clientCount; clientId++) {
            createConnection(clientId);
        }
    }

    private void createConnection(int id) {
        CompletableFuture<ClientConnection> connection = connectionPool.getConnection(id, new InetSocketAddress(mqttHost, mqttPort));
        connection.whenComplete((clientConnection, throwable) -> {
            clientConnection.connectionFuture().whenComplete((unused, connectionThrowable) -> {
                if (Objects.nonNull(connectionThrowable)) {
                    String clientId = clientConnection.getClientId();
                    connectionPool.getFailClientIds().add(Integer.parseInt(clientId));
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
