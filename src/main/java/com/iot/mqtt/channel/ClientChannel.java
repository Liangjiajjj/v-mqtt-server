package com.iot.mqtt.channel;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 客户端链接(session与channel隔离)
 */
@Slf4j
@Getter
@Builder
@AllArgsConstructor
public class ClientChannel {

    private final String clientId;

    private final MqttEndpoint endpoint;

    private final EventExecutor executor;

    public boolean isCleanSession() {
        return endpoint.isCleanSession();
    }

    public void close() {
        endpoint.close();
    }

    /**
     * 广播出去，有可能是不是自己的服务器
     * @param topic
     * @param payload
     * @param qosLevel
     * @param isDup
     * @param isRetain
     * @param messageId
     * @return
     */
    public Future<Integer> publish(String topic, Buffer payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return Future.failedFuture(" connect close !!! ");
        }
        return endpoint.publish(topic, payload, qosLevel, isDup, isRetain, messageId);
    }

    public Future<Integer> publish(String topic, Buffer payload, MqttQoS qosLevel, boolean isDup, boolean isRetain) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return Future.failedFuture(" connect close !!! ");
        }
        return endpoint.publish(topic, payload, qosLevel, isDup, isRetain);
    }

    public void unsubscribeAcknowledge(int messageId) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return;
        }
        endpoint.unsubscribeAcknowledge(messageId);
    }

    public void publishAcknowledge(int messageId) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return;
        }
        endpoint.publishAcknowledge(messageId);
    }

    public void publishRelease(int messageId) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return;
        }
        endpoint.publishRelease(messageId);
    }

    public void subscribeAcknowledge(int messageId, List<MqttQoS> qosLevels) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return;
        }
        endpoint.subscribeAcknowledge(messageId, qosLevels);
    }

    public void publishReceived(int messageId) {
        if (!endpoint.isConnected()) {
            log.error("connect close !!! clientId {} ", clientId);
            return;
        }
        endpoint.publishReceived(messageId);
    }
}
