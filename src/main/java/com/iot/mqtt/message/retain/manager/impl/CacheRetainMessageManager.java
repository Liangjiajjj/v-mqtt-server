package com.iot.mqtt.message.retain.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
public class CacheRetainMessageManager implements IRetainMessageManager {

    private final Map<String, MqttPublishMessage> TOPIC_TO_RETAIN_MESSAGE_MAP = new ConcurrentHashMap<>();

    @Override
    public void put(String topic, MqttPublishMessage retainMessageStore) {
        if (StrUtil.contains(topic, '#') || StrUtil.contains(topic, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        TOPIC_TO_RETAIN_MESSAGE_MAP.put(topic, retainMessageStore);
    }

    @Override
    public MqttPublishMessage get(String topic) {
        return TOPIC_TO_RETAIN_MESSAGE_MAP.get(topic);
    }

    @Override
    public void remove(String topic) {
        TOPIC_TO_RETAIN_MESSAGE_MAP.remove(topic);
    }

    @Override
    public boolean containsKey(String topic) {
        return TOPIC_TO_RETAIN_MESSAGE_MAP.containsKey(topic);
    }

    @Override
    public List<MqttPublishMessage> search(String topicFilter) {
        return Optional.ofNullable(TOPIC_TO_RETAIN_MESSAGE_MAP.get(topicFilter))
                .map(Collections::singletonList)
                .orElse(Collections.EMPTY_LIST);
    }
}
