package com.iot.mqtt.message.retain.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
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
public class RedisRetainMessageManagerImpl extends RedisBaseServiceImpl<PublishMessageStore> implements IRetainMessageManager, RedisBaseService<PublishMessageStore> {

    @Override
    public void put(String topicFilter, MqttPublishMessage retainMessage) {
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        putMap(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter, PublishMessageStore.fromMessage(retainMessage));
    }

    @Override
    public MqttPublishMessage get(String topicFilter) {
        return getMapValue(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter).toMessage();
    }

    @Override
    public void remove(String topicFilter) {
        removeMap(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter);
    }

    @Override
    public boolean containsKey(String topicFilter) {
        return getMap(RedisKeyConstant.RETAIN_KEY.getKey()).containsKey(topicFilter);
    }

    @Override
    public List<MqttPublishMessage> search(String topicFilter) {
        return Optional.ofNullable(getMapValue(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter))
                .map(Collections::singletonList)
                .orElse(Collections.EMPTY_LIST);
    }

}
