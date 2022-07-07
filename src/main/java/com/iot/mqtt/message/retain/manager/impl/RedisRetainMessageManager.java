package com.iot.mqtt.message.retain.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisRetainMessageManager implements IRetainMessageManager {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void put(String topicFilter, MqttPublishMessage retainMessage) {
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        getRMap().put(topicFilter, PublishMessageStore.fromMessage(retainMessage));
    }

    @Override
    public MqttPublishMessage get(String topicFilter) {
        return getRMap().get(topicFilter).toMessage();
    }

    @Override
    public void remove(String topicFilter) {
        getRMap().remove(topicFilter);
    }

    @Override
    public boolean containsKey(String topicFilter) {
        return getRMap().containsKey(topicFilter);
    }

    @Override
    public List<MqttPublishMessage> search(String topicFilter) {
        return Optional.ofNullable(getRMap().get(topicFilter))
                .map(Collections::singletonList)
                .orElse(Collections.EMPTY_LIST);
    }

    private RMap<String, PublishMessageStore> getRMap() {
        return redissonClient.getMap(RedisKeyConstant.RETAIN_KEY.getKey());
    }
}
