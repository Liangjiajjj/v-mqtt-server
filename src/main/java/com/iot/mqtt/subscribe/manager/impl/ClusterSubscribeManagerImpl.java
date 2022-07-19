package com.iot.mqtt.subscribe.manager.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.context.MqttServiceContext;
import com.iot.mqtt.filter.FixedTopicFilter;
import com.iot.mqtt.filter.TopicFilter;
import com.iot.mqtt.filter.TreeTopicFilter;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.annotation.RedisBatch;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.retain.manager.IRetainMessageManager;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.Subscribe;
import com.iot.mqtt.subscribe.SubscribeOperation;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import com.iot.mqtt.type.OperationType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class ClusterSubscribeManagerImpl extends RedisBaseServiceImpl<Subscribe> implements ISubscribeManager, RedisBaseService<Subscribe> {

    private static final String ONE_SYMBOL = "+";

    private static final String MORE_SYMBOL = "#";

    private TopicFilter<Subscribe> fixedTopicFilter;

    private TopicFilter<Subscribe> treeTopicFilter;

    @Autowired
    private MqttConfig config;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MqttServiceContext mqttServiceContext;

    @Autowired
    private IRetainMessageManager retainMessageManager;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Resource(name = "PUBLISH-EXECUTOR")
    protected MqttEventExecuteGroup mqttEventExecuteGroup;

    @PostConstruct
    private void init() {
        this.fixedTopicFilter = new FixedTopicFilter<>();
        this.treeTopicFilter = new TreeTopicFilter<>();
        initRedis();
    }

    private void initRedis() {
        // 从redis上面加载topic
        getKeys().getKeysByPattern(RedisKeyConstant.SUBSCRIBE_KEY.getKey("*")).forEach((key) -> {
            for (Subscribe subscribe : getMap(key).values()) {
                add0(subscribe);
            }
        });
        // todo:先用redis传递，以后改用mq
        getTopic(RedisKeyConstant.SYN_SUBSCRIBE_TOPIC.getKey()).addListener(SubscribeOperation.class, (channel, operation) -> {
            // 本服务器不需要处理
            if (config.getBrokerId().equals(operation.getBrokerId())) {
                log.info("syn subscribe topic not handle self !!!  operation brokerId :{} , brokerId:{}  ", operation.getBrokerId(), config.getBrokerId());
                return;
            }
            OperationType operationType = OperationType.getOperationType(operation.getOperation());
            if (log.isTraceEnabled()) {
                log.trace("syn subscribe topic operation:{} , subscribe:{}  ", operationType, operation.getSubscribe());
            }
            if (OperationType.ADD.equals(operationType)) {
                add0(operation.getSubscribe());
            } else {
                remove0(operation.getSubscribe());
            }
        });
    }

    @Override
    public void add(Subscribe subscribe) {
        log.debug("Subscription ClientId {} for {} with QoS  {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
        add0(subscribe);
        putMap(RedisKeyConstant.SUBSCRIBE_KEY.getKey(subscribe.getTopicFilter()), subscribe.getClientId(), subscribe);
        addSet(RedisKeyConstant.SUBSCRIBE_SET_KEY.getKey(subscribe.getClientId()), subscribe.getTopicFilter());
        synSubscribeTopic(OperationType.ADD, subscribe);
    }

    @Override
    public void remove(Subscribe subscribe) {
        log.debug("Unsubscription ClientId {} for {} ", subscribe.getClientId(), subscribe.getTopicFilter());
        remove0(subscribe);
        removeMap(RedisKeyConstant.SUBSCRIBE_KEY.getKey(subscribe.getTopicFilter()), subscribe.getClientId());
        removeSet(RedisKeyConstant.SUBSCRIBE_SET_KEY.getKey(subscribe.getClientId()), subscribe.getTopicFilter());
        synSubscribeTopic(OperationType.REMOVE, subscribe);
    }

    @Override
    public void removeForClient(String clientId) {
        String subscribeSetKey = RedisKeyConstant.SUBSCRIBE_SET_KEY.getKey(clientId);
        getSet(subscribeSetKey).whenCompleteAsync((set, throwable) -> {
            List<Subscribe> subscribes = set.stream().map(o -> (String) o)
                    .map((topicFilter) ->
                            Subscribe.builder().clientId(clientId).topicFilter(topicFilter).build()
                    ).collect(Collectors.toList());
            context.getBean(this.getClass()).removeSubscriptions(subscribes);
        });
        // removeSet(subscribeSetKey);
    }

    @Override
    public Collection<Subscribe> search(String topicName) {
        Set<Subscribe> subscribeTopics = fixedTopicFilter.getSet(topicName);
        subscribeTopics.addAll(treeTopicFilter.getSet(topicName));
        return subscribeTopics;
    }

    @Override
    // @RedisBatch
    public void publishSubscribes(ClientChannel clientChannel, MqttPublishMessage message) {
        long startTime = System.currentTimeMillis();
        String topicName = message.variableHeader().topicName();
        MqttQoS mqttQoS = message.fixedHeader().qosLevel();
        retainMessageManager.handlerRetainMessage(message, topicName);
        Collection<Subscribe> subscribes = search(topicName);
        subscribes.forEach(subscribe -> {
            MqttPublishMessage copyMessage = message.copy();
            mqttEventExecuteGroup.get(subscribe.getMd5Key()).execute(() -> {
                IQosLevelMessageService qosLevelMessageService = mqttServiceContext.getQosLevelMessageService(mqttQoS);
                qosLevelMessageService.publish(clientChannel, subscribe, copyMessage);
            });
        });
        log.info("publishSubscribes subscribes size {} , time : {} 's ", subscribes.size(), (System.currentTimeMillis() - startTime) / 1000d);
    }

    private void add0(Subscribe subscribe) {
        if (subscribe.getTopicFilter().contains(ONE_SYMBOL) || subscribe.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.add(subscribe);
        } else {
            fixedTopicFilter.add(subscribe);
        }
    }

    private void remove0(Subscribe subscribe) {
        if (subscribe.getTopicFilter().contains(ONE_SYMBOL) || subscribe.getTopicFilter().contains(MORE_SYMBOL)) {
            treeTopicFilter.remove(subscribe);
        } else {
            fixedTopicFilter.remove(subscribe);
        }
    }

    public void synSubscribeTopic(OperationType type, Subscribe subscribe) {
        SubscribeOperation operation = SubscribeOperation.builder().operation(type.getType()).subscribe(subscribe).brokerId(config.getBrokerId()).build();
        publish(RedisKeyConstant.SYN_SUBSCRIBE_TOPIC.getKey(), operation);
    }

    @Override
    @RedisBatch
    public void addSubscriptions(String clientId, Long md5Key, MqttSubscribeMessage message) {
        ISubscribeManager.super.addSubscriptions(clientId, md5Key, message);
    }

    @Override
    @RedisBatch
    public void removeSubscriptions(String clientId, MqttUnsubscribeMessage message) {
        ISubscribeManager.super.removeSubscriptions(clientId, message);
    }

    @Override
    @RedisBatch
    public void removeSubscriptions(List<Subscribe> subscribes) {
        ISubscribeManager.super.removeSubscriptions(subscribes);
    }
}
