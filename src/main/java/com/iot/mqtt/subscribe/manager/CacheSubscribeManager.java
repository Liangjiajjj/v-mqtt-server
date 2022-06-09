package com.iot.mqtt.subscribe.manager;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.subscribe.Subscribe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅管理
 *
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "false")
public class CacheSubscribeManager implements ISubscribeManager {
    /**
     * 订阅 列表
     * topicFilter -> map<clientId,Subscribe>
     */
    private final static Map<String, Map<String, Subscribe>> TOPIC_FILTERTO_SUBSCRIBE_MAP = new ConcurrentHashMap<>();
    /**
     * clientId -> set<topicFilter>
     */
    private final static Map<String, Set<String>> CLIENT_IDTO_TOPICS_MAP = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, Subscribe subscribeStore) {
        String topicFilter = subscribeStore.getTopicFilter();
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        CLIENT_IDTO_TOPICS_MAP.computeIfAbsent(clientId, (m) -> new HashSet<>()).add(topicFilter);
        TOPIC_FILTERTO_SUBSCRIBE_MAP
                .computeIfAbsent(topicFilter, (m) -> new ConcurrentHashMap<>(16))
                .put(clientId, subscribeStore);
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        Optional.ofNullable(TOPIC_FILTERTO_SUBSCRIBE_MAP.get(topicFilter)).ifPresent((map -> map.remove(clientId)));
        Optional.ofNullable(CLIENT_IDTO_TOPICS_MAP.get(clientId)).ifPresent(set -> set.remove(topicFilter));
    }

    @Override
    public void removeForClient(String clientId) {
        CLIENT_IDTO_TOPICS_MAP.remove(clientId);
        TOPIC_FILTERTO_SUBSCRIBE_MAP.forEach((topicFilter, subscribeMap) -> subscribeMap.remove(clientId));
    }

    @Override
    public Collection<Subscribe> search(String topicFilter) {
        return TOPIC_FILTERTO_SUBSCRIBE_MAP.getOrDefault(topicFilter, Collections.EMPTY_MAP).values();
    }
}
