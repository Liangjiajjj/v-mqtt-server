package com.iot.mqtt.subscribe.topic;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author liangjiajun
 */
public class TreeTopicFilter implements TopicFilter {

    private final TreeNode rootTreeNode = new TreeNode("root");

    private final LongAdder subscribeNumber = new LongAdder();

    @Override
    public Set<Subscribe> getSubscribeByTopic(String topic) {
        return new HashSet<>(rootTreeNode.getSubscribeByTopic(topic));
    }

    @Override
    public void addSubscribeTopic(String topicFilter, String clientId, MqttQoS mqttQoS) {
        this.addSubscribeTopic(new Subscribe(clientId, topicFilter, mqttQoS.value()));
    }

    @Override
    public void addSubscribeTopic(Subscribe subscribe) {
        if (rootTreeNode.addSubscribeTopic(subscribe)) {
            subscribeNumber.add(1);
        }
    }

    @Override
    public void removeSubscribeTopic(Subscribe subscribe) {
        if (rootTreeNode.removeSubscribeTopic(subscribe)) {
            subscribeNumber.add(-1);
        }
    }

    @Override
    public int count() {
        return (int) subscribeNumber.sum();
    }

    @Override
    public Set<Subscribe> getAllSubscribesTopic() {
        return rootTreeNode.getAllSubscribesTopic();
    }


}
