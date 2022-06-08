package com.iot.mqtt.message.dup.manager;

import io.vertx.mqtt.messages.MqttPublishMessage;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheDupPublishMessageManager implements IDupPublishMessageManager {

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, MqttPublishMessage>> clientId2DupPublishMessageMap = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, MqttPublishMessage publishMessage) {
        Objects.requireNonNull(clientId2DupPublishMessageMap.computeIfPresent(clientId, ((s, map) -> new ConcurrentHashMap<>()))).
                put(publishMessage.messageId(), publishMessage);
    }

    @Override
    public Collection<MqttPublishMessage> get(String clientId) {
        return Optional.ofNullable(clientId2DupPublishMessageMap.get(clientId))
                .orElseGet(ConcurrentHashMap::new).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        Optional.ofNullable(clientId2DupPublishMessageMap.get(clientId)).ifPresent((map)-> map.remove(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        clientId2DupPublishMessageMap.remove(clientId);
    }
}
