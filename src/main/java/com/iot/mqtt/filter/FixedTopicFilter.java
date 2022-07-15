package com.iot.mqtt.filter;


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
public class FixedTopicFilter<T extends BaseTopicBean> implements TopicFilter<T> {

    private final LongAdder count = new LongAdder();

    private final Map<String, CopyOnWriteArraySet<T>> topicChannels = new ConcurrentHashMap<>();

    @Override
    public Set<T> getSet(String topic) {
        CopyOnWriteArraySet<T> channels = topicChannels.computeIfAbsent(topic, t -> new CopyOnWriteArraySet<>());
        return new HashSet<>(channels);
    }

    @Override
    public void add(T t) {
        CopyOnWriteArraySet<T> channels = topicChannels.computeIfAbsent(t.getTopicFilter(), list -> new CopyOnWriteArraySet<>());
        if (channels.add(t)) {
            count.add(1);
        }
    }

    @Override
    public void remove(T t) {
        CopyOnWriteArraySet<T> channels = topicChannels.computeIfAbsent(t.getTopicFilter(), list -> new CopyOnWriteArraySet<>());
        if (channels.remove(t)) {
            count.add(-1);
        }
    }

    @Override
    public int count() {
        return (int) count.sum();
    }

    @Override
    public Set<T> getAllSet() {
        return topicChannels.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
