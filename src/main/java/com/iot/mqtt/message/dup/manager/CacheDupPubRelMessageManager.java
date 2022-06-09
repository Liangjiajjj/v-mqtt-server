package com.iot.mqtt.message.dup.manager;


import com.iot.mqtt.message.dup.DupPubRelMessage;
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
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "false")
public class CacheDupPubRelMessageManager implements IDupPubRelMessageManager {

    private final static ConcurrentHashMap<String, ConcurrentHashMap<Integer, DupPubRelMessage>> CLIENT_ID_TO_DUP_PUB_REL_MESSAGE_MAP = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, DupPubRelMessage publishMessage) {
        Objects.requireNonNull(CLIENT_ID_TO_DUP_PUB_REL_MESSAGE_MAP.computeIfAbsent(clientId, ((map) -> new ConcurrentHashMap<>(16)))).
                put(publishMessage.getMessageId(), publishMessage);
    }

    @Override
    public Collection<DupPubRelMessage> get(String clientId) {
        return Optional.ofNullable(CLIENT_ID_TO_DUP_PUB_REL_MESSAGE_MAP.get(clientId))
                .orElseGet(ConcurrentHashMap::new).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        Optional.ofNullable(CLIENT_ID_TO_DUP_PUB_REL_MESSAGE_MAP.get(clientId)).ifPresent((map)-> map.remove(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        CLIENT_ID_TO_DUP_PUB_REL_MESSAGE_MAP.remove(clientId);
    }
}
