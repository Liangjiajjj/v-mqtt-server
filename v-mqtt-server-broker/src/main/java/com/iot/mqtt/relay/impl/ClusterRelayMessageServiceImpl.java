package com.iot.mqtt.relay.impl;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.event.SessionRefreshEvent;
import com.iot.mqtt.relay.connection.RelayConnection;
import com.iot.mqtt.relay.connection.RelayConnectionPool;
import com.iot.mqtt.relay.api.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.api.IClientSessionManager;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 在服务存建的服务端和客户端转发
 *
 * @author liangjiajun
 */
@Slf4j
@Service
@Conditional(ClusterRelayMessageServiceImpl.ClusterRelayProperty.class)
public class ClusterRelayMessageServiceImpl implements IRelayMessageService {

    @Resource
    private MqttConfig mqttConfig;

    @Resource(name = "RELAY-PUBLISH-CLIENT-EXECUTOR")
    private MqttEventExecuteGroup relayPublishClientExecutor;

    /**
     * 转发连接池
     */
    @Resource
    private RelayConnectionPool relayConnectionPool;

    @Resource
    private IClientSessionManager clientSessionManager;

    @Override
    public void relayMessage(ClientSession clientSession, int messageId, MqttPublishMessage message, CompletableFuture<Void> future) {
        String brokerId = clientSession.getBrokerId();
        String clientId = clientSession.getClientId();
        try {
            String url = mqttConfig.getBrokerId2UrlMap().get(brokerId);
            InetSocketAddress address = new InetSocketAddress(url.split(":")[0], Integer.parseInt(url.split(":")[1]));
            // 每个客户端hash到一个转发连接
            relayConnectionPool.getConnection(clientSession.getMd5Key(), address).whenComplete((connection, throwable) -> {
                relayMessage0(connection, clientSession, message, future);
            });
        } catch (Exception e) {
            log.error("relayMessage error !!! brokerId {} , clientId {} ,", brokerId, clientId, e);
        }
    }

    private void relayMessage0(RelayConnection connection, ClientSession clientSession, MqttPublishMessage message, CompletableFuture<Void> future) {
        relayPublishClientExecutor.get(clientSession.getMd5Key()).execute(() -> {
            try {
                connection.relayPublish(clientSession.getClientId(), message);
                future.complete(null);
                log.info("connection relayMessage brokerId {} , clientId {} ,", clientSession.getBrokerId(), clientSession.getClientId());
            } catch (Exception e) {
                log.error("connection relayMessage error !!! brokerId {} , clientId {} ,", clientSession.getBrokerId(), clientSession.getClientId(), e);
            }
        });
    }

    /**
     * session变化是应该推送完所有的数据给它
     *
     * @param event
     */
    @EventListener
    public void onSessionRefreshEvent(SessionRefreshEvent event) {
        if (mqttConfig.getIsBatchRelay() && mqttConfig.getIsRedisKeyNotify()) {
            ClientSession clientSession = clientSessionManager.get(event.getClientId());
            if (Objects.nonNull(clientSession)) {
                String url = mqttConfig.getBrokerId2UrlMap().get(clientSession.getBrokerId());
                InetSocketAddress address = new InetSocketAddress(url.split(":")[0], Integer.parseInt(url.split(":")[1]));
                CompletableFuture<RelayConnection> completableFuture = relayConnectionPool.getConnection(clientSession.getMd5Key(), address, false);
                if (Objects.nonNull(completableFuture)) {
                    completableFuture.whenComplete((relayConnection, throwable) -> relayConnection.flush());
                }
            }
        }
    }

    static class ClusterRelayProperty extends AllNestedConditions {

        public ClusterRelayProperty() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
        static class ClusterEnabled {
        }

        @ConditionalOnProperty(name = "mqtt.is_open_relay_server", havingValue = "true")
        static class OpenRelayServer {
        }
    }
}

