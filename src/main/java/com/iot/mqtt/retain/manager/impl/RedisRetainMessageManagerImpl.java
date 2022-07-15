package com.iot.mqtt.retain.manager.impl;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.retain.RetainMessageOperation;
import com.iot.mqtt.retain.manager.IRetainMessageManager;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.annotation.RedisBatch;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.filter.FixedTopicFilter;
import com.iot.mqtt.filter.TopicFilter;
import com.iot.mqtt.filter.TreeTopicFilter;
import com.iot.mqtt.type.OperationType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisRetainMessageManagerImpl extends RedisBaseServiceImpl<PublishMessageStore> implements IRetainMessageManager, RedisBaseService<PublishMessageStore> {

    private static final String ONE_SYMBOL = "+";

    private static final String MORE_SYMBOL = "#";

    private TopicFilter<PublishMessageStore> fixedTopicFilter;

    private TopicFilter<PublishMessageStore> treeTopicFilter;

    @Autowired
    private MqttConfig config;

    @PostConstruct
    private void init() {
        this.fixedTopicFilter = new FixedTopicFilter<>();
        this.treeTopicFilter = new TreeTopicFilter<>();
        initRedis();
    }

    private void initRedis() {
        // 从redis上面加载topic
        getKeys().getKeysByPattern(RedisKeyConstant.RETAIN_KEY.getKey()).forEach((key) -> {
            for (PublishMessageStore publishMessage : getMap(key).values()) {
                add0(publishMessage);
            }
        });
        // todo:先用redis传递，以后改用mq
        getTopic(RedisKeyConstant.SYN_RETAIN_MESSAGE_TOPIC.getKey()).addListener(RetainMessageOperation.class, (channel, operation) -> {
            // 本服务器不需要处理
            if (config.getBrokerId().equals(operation.getBrokerId())) {
                log.info("syn retain_message topic not handle self !!!  operation brokerId :{} , brokerId:{}  ", operation.getBrokerId(), config.getBrokerId());
                return;
            }
            OperationType operationType = OperationType.getOperationType(operation.getOperation());
            if (log.isTraceEnabled()) {
                log.trace("syn retain_message topic operation:{} , subscribe:{}  ", operationType, operation.getPublishMessageStore());
            }
            if (OperationType.ADD.equals(operationType)) {
                add0(operation.getPublishMessageStore());
            } else {
                remove0(operation.getPublishMessageStore());
            }
        });
    }

    @Override
    @RedisBatch
    public void put(String topicFilter, MqttPublishMessage retainMessage) {
        PublishMessageStore publishMessageStore = PublishMessageStore.fromMessage(retainMessage);
        add0(publishMessageStore);
        putMap(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter, publishMessageStore);
        synRetainTopic(OperationType.ADD, publishMessageStore);
    }

    @Override
    public PublishMessageStore get(String topicFilter) {
        return getMapValue(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter);
    }

    @Override
    @RedisBatch
    public void remove(String topicFilter) {
        PublishMessageStore publishMessageStore = PublishMessageStore.fromMessage(topicFilter);
        remove0(publishMessageStore);
        removeMap(RedisKeyConstant.RETAIN_KEY.getKey(), topicFilter);
        synRetainTopic(OperationType.REMOVE, publishMessageStore);
    }

    @Override
    public boolean containsKey(String topicFilter) {
        return getMap(RedisKeyConstant.RETAIN_KEY.getKey()).containsKey(topicFilter);
    }

    @Override
    public List<PublishMessageStore> search(String topicFilter) {
        Set<PublishMessageStore> subscribeTopics = fixedTopicFilter.getSet(topicFilter);
        subscribeTopics.addAll(treeTopicFilter.getSet(topicFilter));
        return new ArrayList<>(subscribeTopics);
    }

    private void add0(PublishMessageStore publishMessage) {
        if (publishMessage.getTopicFilter().contains(ONE_SYMBOL) || publishMessage.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.add(publishMessage);
        } else {
            fixedTopicFilter.add(publishMessage);
        }
    }

    private void remove0(PublishMessageStore publishMessage) {
        if (publishMessage.getTopicFilter().contains(ONE_SYMBOL) || publishMessage.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.remove(publishMessage);
        } else {
            fixedTopicFilter.remove(publishMessage);
        }
    }

    public void synRetainTopic(OperationType type, PublishMessageStore publishMessageStore) {
        RetainMessageOperation operation = RetainMessageOperation.builder().operation(type.getType())
                .publishMessageStore(publishMessageStore).brokerId(config.getBrokerId()).build();
        publish(RedisKeyConstant.SYN_RETAIN_MESSAGE_TOPIC.getKey(), operation);
    }
}
