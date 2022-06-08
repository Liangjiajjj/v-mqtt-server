package com.iot.mqtt.message.retain.manager;

import cn.hutool.core.util.StrUtil;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheRetainMessageManager implements IRetainMessageManager {

    private Map<String, MqttPublishMessage> topic2RetainMessageMap = new ConcurrentHashMap<>();

    @Override
    public void put(String topic, MqttPublishMessage retainMessageStore) {
        if (StrUtil.contains(topic, '#') || StrUtil.contains(topic, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        topic2RetainMessageMap.put(topic, retainMessageStore);
    }

    @Override
    public MqttPublishMessage get(String topic) {
        return topic2RetainMessageMap.get(topic);
    }

    @Override
    public void remove(String topic) {
        topic2RetainMessageMap.remove(topic);
    }

    @Override
    public boolean containsKey(String topic) {
        return topic2RetainMessageMap.containsKey(topic);
    }

    @Override
    public List<MqttPublishMessage> search(String topicFilter) {
        return Collections.singletonList(topic2RetainMessageMap.get(topicFilter));
    }
}
