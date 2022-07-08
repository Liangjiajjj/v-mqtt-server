package com.iot.mqtt.subscribe.topic;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
public class FixedTopicFilter implements TopicFilter {

    private final LongAdder subscribeNumber = new LongAdder();

    private final Map<String, CopyOnWriteArraySet<Subscribe>> topicChannels = new ConcurrentHashMap<>();


    @Override
    public Set<Subscribe> getSubscribeByTopic(String topic) {
        CopyOnWriteArraySet<Subscribe> channels = topicChannels.computeIfAbsent(topic, t -> new CopyOnWriteArraySet<>());
        return new HashSet<>(channels);
    }

    @Override
    public void addSubscribeTopic(String topicFilter, String clientId, MqttQoS mqttQoS) {
        this.addSubscribeTopic(new Subscribe(clientId, topicFilter, mqttQoS.value()));
    }

    @Override
    public void addSubscribeTopic(Subscribe subscribe) {
        CopyOnWriteArraySet<Subscribe> channels = topicChannels.computeIfAbsent(subscribe.getTopicFilter(), t -> new CopyOnWriteArraySet<>());
        if (channels.add(subscribe)) {
            subscribeNumber.add(1);
        }
    }

    @Override
    public void removeSubscribeTopic(Subscribe subscribe) {
        CopyOnWriteArraySet<Subscribe> channels = topicChannels.computeIfAbsent(subscribe.getTopicFilter(), t -> new CopyOnWriteArraySet<>());
        if (channels.remove(subscribe)) {
            subscribeNumber.add(-1);
        }
    }

    @Override
    public int count() {
        return (int) subscribeNumber.sum();
    }

    @Override
    public Set<Subscribe> getAllSubscribesTopic() {
        return topicChannels.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
