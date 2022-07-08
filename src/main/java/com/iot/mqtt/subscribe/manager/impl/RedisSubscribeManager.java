/*
package com.iot.mqtt.subscribe.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.topic.Subscribe;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

*/
/**
 * 订阅管理
 *
 * @author liangjiajun
 *//*

@Service
@ConditionalOnExpression("${mqtt.cluster_enabled:true}&&${mqtt.wild_car:false}")
public class RedisSubscribeManager implements ISubscribeManager {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void put(String clientId, Subscribe subscribe) {
        String topicFilter = subscribe.getTopicFilter();
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        getRMap(subscribe.getTopicFilter()).put(clientId, subscribe);
        getRSet(clientId).add(subscribe.getTopicFilter());
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        getRMap(topicFilter).remove(clientId);
        getRSet(clientId).add(topicFilter);
    }

    @Override
    public void removeForClient(String clientId) {
        for (String topicFilter : getRSet(clientId)) {
            getRMap(topicFilter).remove(clientId);
        }
        getRSet(clientId).delete();
    }

    @Override
    public Collection<Subscribe> search(String topicFilter) {
        return Optional.ofNullable(getRMap(topicFilter))
                .map(smap -> smap.values())
                .orElse(Collections.EMPTY_LIST);
    }

    private RMap<String, Subscribe> getRMap(String topicFilter) {
        return redissonClient.getMap(RedisKeyConstant.SUBSCRIBE_KEY.getKey(topicFilter));
    }

    private RSet<String> getRSet(String clientId) {
        return redissonClient.getSet(RedisKeyConstant.SUBSCRIBE_SET_KEY.getKey(clientId));
    }
}
*/
