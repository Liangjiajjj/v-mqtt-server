package com.iot.mqtt.message.dup.manager;


import com.iot.mqtt.message.dup.DupPubRelMessage;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheDupPubRelMessageManager implements IDupPubRelMessageManager {

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, DupPubRelMessage>> clientId2DupPubRelMessageMap = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, DupPubRelMessage publishMessage) {
        Objects.requireNonNull(clientId2DupPubRelMessageMap.computeIfPresent(clientId, ((s, map) -> new ConcurrentHashMap<>()))).
                put(publishMessage.getMessageId(), publishMessage);
    }

    @Override
    public Collection<DupPubRelMessage> get(String clientId) {
        return Optional.ofNullable(clientId2DupPubRelMessageMap.get(clientId))
                .orElseGet(ConcurrentHashMap::new).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        Optional.ofNullable(clientId2DupPubRelMessageMap.get(clientId)).ifPresent((map)-> map.remove(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        clientId2DupPubRelMessageMap.remove(clientId);
    }
}
