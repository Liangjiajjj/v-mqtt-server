package com.iot.mqtt.message.dup.manager.impl;

import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
public class CacheDupPublishMessageManager implements IDupPublishMessageManager {

    private final static ConcurrentHashMap<String, ConcurrentHashMap<Integer, MqttPublishMessage>> CLIENT_ID_TO_DUP_PUBLISH_MESSAGE_MAP = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, MqttPublishMessage publishMessage) {
        Objects.requireNonNull(CLIENT_ID_TO_DUP_PUBLISH_MESSAGE_MAP.computeIfAbsent(clientId, ((m) -> new ConcurrentHashMap<>(16)))).
                put(publishMessage.variableHeader().messageId(), publishMessage);
    }

    @Override
    public Collection<MqttPublishMessage> get(String clientId) {
        return Optional.ofNullable(CLIENT_ID_TO_DUP_PUBLISH_MESSAGE_MAP.get(clientId))
                .orElseGet(ConcurrentHashMap::new).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        Optional.ofNullable(CLIENT_ID_TO_DUP_PUBLISH_MESSAGE_MAP.get(clientId)).ifPresent((map) -> map.remove(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        CLIENT_ID_TO_DUP_PUBLISH_MESSAGE_MAP.remove(clientId);
    }
}
