package com.iot.mqtt.subscribe.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.context.MqttServiceContext;
import com.iot.mqtt.retain.api.IRetainMessageManager;
import com.iot.mqtt.subscribe.api.ISubscribeManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅管理
 *
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
public class CacheSubscribeManager implements ISubscribeManager {

    @Resource
    private MqttServiceContext mqttServiceContext;

    @Resource
    private IRetainMessageManager retainMessageManager;
    /**
     * 订阅 列表
     * topicFilter -> map<clientId,Subscribe>
     */
    private final static Map<String, Map<String, Subscribe>> TOPIC_FILTERTO_SUBSCRIBE_MAP = new ConcurrentHashMap<>();

    @Override
    public void add(Subscribe subscribe) {
        log.debug("Subscription ClientId {} for {} with QoS  {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
        String clientId = subscribe.getClientId();
        String topicFilter = subscribe.getTopicFilter();
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            throw new RuntimeException("暂时不支持表达式 topic !!!");
        }
        TOPIC_FILTERTO_SUBSCRIBE_MAP
                .computeIfAbsent(topicFilter, (m) -> new ConcurrentHashMap<>(16))
                .put(clientId, subscribe);
    }

    @Override
    public void remove(Subscribe subscribe) {
        log.debug("Unsubscription ClientId {} for {} ", subscribe.getClientId(), subscribe.getTopicFilter());
        Optional.ofNullable(TOPIC_FILTERTO_SUBSCRIBE_MAP.get(subscribe.getTopicFilter())).ifPresent((map -> map.remove(subscribe.getClientId())));
    }

    @Override
    public void removeForClient(String clientId) {
        TOPIC_FILTERTO_SUBSCRIBE_MAP.forEach((topicFilter, subscribeMap) -> subscribeMap.remove(clientId));
    }

    @Override
    public Collection<Subscribe> search(String topicFilter) {
        return TOPIC_FILTERTO_SUBSCRIBE_MAP.getOrDefault(topicFilter, Collections.EMPTY_MAP).values();
    }

    @Override
    public void publishSubscribes(ClientChannel clientChannel, MqttPublishMessage message) {
      /*  String topicName = message.variableHeader().topicName();
        MqttQoS mqttQoS = message.fixedHeader().qosLevel();
        Collection<Subscribe> subscribes = search(topicName);
        retainMessageManager.handlerRetainMessage(message, topicName);
        subscribes.forEach(subscribe -> {
            // 发送消息到订阅的topic
            IQosLevelMessageService qosLevelMessageService = mqttServiceContext.getQosLevelMessageService(mqttQoS);
            qosLevelMessageService.publish(clientChannel, subscribe, message);
        });*/
    }
}
