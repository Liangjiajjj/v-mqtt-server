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
    private final static Map<String, Map<String, Subscribe>> topicFilter2SubscribeMap = new ConcurrentHashMap<>();
    /**
     * clientId -> set<topicFilter>
     */
    private final static Map<String, Set<String>> clientId2TopicsMap = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, Subscribe subscribeStore) {
        String topicFilter = subscribeStore.getTopicFilter();
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        clientId2TopicsMap.computeIfAbsent(clientId, (m) -> new HashSet<>()).add(topicFilter);
        topicFilter2SubscribeMap
                .computeIfAbsent(topicFilter, (m) -> new ConcurrentHashMap<>(16))
                .put(clientId, subscribeStore);
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        Optional.ofNullable(topicFilter2SubscribeMap.get(topicFilter)).ifPresent((map -> map.remove(clientId)));
        Optional.ofNullable(clientId2TopicsMap.get(clientId)).ifPresent(set -> set.remove(topicFilter));
    }

    @Override
    public void removeForClient(String clientId) {
        clientId2TopicsMap.remove(clientId);
        topicFilter2SubscribeMap.forEach((topicFilter, subscribeMap) -> subscribeMap.remove(clientId));
    }

    @Override
    public Collection<Subscribe> search(String topicFilter) {
        return topicFilter2SubscribeMap.getOrDefault(topicFilter, Collections.EMPTY_MAP).values();
    }
}
