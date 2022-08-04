package com.iot.mqtt.filter;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author liangjiajun
 */
public class TreeTopicFilter<T extends BaseTopicBean> implements TopicFilter<T> {

    private final TreeNode<T> rootTreeNode = new TreeNode<>("root");

    private final LongAdder count = new LongAdder();

    @Override
    public Set<T> getSet(String topic) {
        return new HashSet<>(rootTreeNode.getTopicsByTopic(topic));
    }

    @Override
    public void add(T t) {
        if (rootTreeNode.add(t)) {
            count.add(1);
        }
    }

    @Override
    public void remove(T t) {
        if (rootTreeNode.remove(t)) {
            count.add(-1);
        }
    }

    @Override
    public int count() {
        return (int) count.sum();
    }

    @Override
    public Set<T> getAllSet() {
        return rootTreeNode.getTopicSet();
    }
}
