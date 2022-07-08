package com.iot.mqtt.subscribe.manager.impl;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.topic.*;
import com.iot.mqtt.type.SubscribeOperationType;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Set;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class ClusterSubscribeManager implements ISubscribeManager {

    private static final String ONE_SYMBOL = "+";

    private static final String MORE_SYMBOL = "#";

    @Autowired
    private MqttConfig config;
    @Autowired
    private RedissonClient redissonClient;

    private TopicFilter fixedTopicFilter;

    private TopicFilter treeTopicFilter;

    @PostConstruct
    private void init() {
        this.fixedTopicFilter = new FixedTopicFilter();
        this.treeTopicFilter = new TreeTopicFilter();
        initRedis();
    }

    private void initRedis() {
        // 从redis上面加载topic
        redissonClient.getKeys().getKeysByPattern(RedisKeyConstant.SUBSCRIBE_KEY.getKey("*")).forEach((key) -> {
            for (Subscribe subscribe : getRMapByKey(key).values()) {
                add0(subscribe);
            }
        });
        // todo:先用redis传递，以后改用mq
        redissonClient.getTopic(RedisKeyConstant.SYN_SUBSCRIBE_TOPIC.getKey()).addListener(SubscribeOperation.class, (channel, operation) -> {
            // 本服务器不需要处理
            if (config.getBrokerId().equals(operation.getBrokerId())) {
                log.info("syn subscribe topic not handle self !!!  operation brokerId :{} , brokerId:{}  ", operation.getBrokerId(), config.getBrokerId());
                return;
            }
            SubscribeOperationType operationType = SubscribeOperationType.getSubscribeOperationType(operation.getOperation());
            if (log.isTraceEnabled()) {
                log.trace("syn subscribe topic operation:{} , subscribe:{}  ", operationType, operation.getSubscribe());
            }
            if (SubscribeOperationType.ADD.equals(operationType)) {
                add0(operation.getSubscribe());
            } else {
                remove0(operation.getSubscribe());
            }
        });
    }

    @Override
    public void add(Subscribe subscribe) {
        add0(subscribe);
        getRMap(subscribe.getTopicFilter()).put(subscribe.getClientId(), subscribe);
        getRSet(subscribe.getClientId()).add(subscribe.getTopicFilter());
        synSubscribeTopic(SubscribeOperationType.ADD, subscribe);
    }

    @Override
    public void remove(Subscribe subscribe) {
        remove0(subscribe);
        getRMap(subscribe.getTopicFilter()).remove(subscribe.getClientId());
        getRSet(subscribe.getClientId()).remove(subscribe.getTopicFilter());
        synSubscribeTopic(SubscribeOperationType.REMOVE, subscribe);
    }

    @Override
    public void removeForClient(String clientId) {
        for (String topicFilter : getRSet(clientId)) {
            Subscribe subscribe = Subscribe.builder().clientId(clientId)
                    .topicFilter(topicFilter).build();
            remove(subscribe);
        }
        getRSet(clientId).delete();
    }

    @Override
    public Collection<Subscribe> search(String topicName) {
        Set<Subscribe> subscribeTopics = fixedTopicFilter.getSubscribeByTopic(topicName);
        subscribeTopics.addAll(treeTopicFilter.getSubscribeByTopic(topicName));
        return subscribeTopics;
    }

    private RMap<String, Subscribe> getRMapByKey(String key) {
        return redissonClient.getMap(key);
    }

    private RMap<String, Subscribe> getRMap(String topicFilter) {
        return getRMapByKey(RedisKeyConstant.SUBSCRIBE_KEY.getKey(topicFilter));
    }

    private RSet<String> getRSet(String clientId) {
        return redissonClient.getSet(RedisKeyConstant.SUBSCRIBE_SET_KEY.getKey(clientId));
    }

    private void add0(Subscribe subscribe) {
        if (subscribe.getTopicFilter().contains(ONE_SYMBOL) || subscribe.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.addSubscribeTopic(subscribe);
        } else {
            fixedTopicFilter.addSubscribeTopic(subscribe);
        }
    }

    private void remove0(Subscribe subscribe) {
        if (subscribe.getTopicFilter().contains(ONE_SYMBOL) || subscribe.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.removeSubscribeTopic(subscribe);
        } else {
            fixedTopicFilter.removeSubscribeTopic(subscribe);
        }
    }

    private void synSubscribeTopic(SubscribeOperationType type, Subscribe subscribe) {
        SubscribeOperation operation = SubscribeOperation.builder().operation(type.getType()).subscribe(subscribe).brokerId(config.getBrokerId()).build();
        redissonClient.getTopic(RedisKeyConstant.SYN_SUBSCRIBE_TOPIC.getKey()).publish(operation);
    }
}
