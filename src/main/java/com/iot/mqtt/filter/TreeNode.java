package com.iot.mqtt.filter;

import cn.hutool.core.collection.ConcurrentHashSet;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
@Getter
@Setter
public class TreeNode<T extends BaseTopicBean> {

    private final String ONE_SYMBOL = "+";

    private final String MORE_SYMBOL = "#";

    private final String topic;

    private Set<T> topicSet = new ConcurrentHashSet<>();

    private Map<String, TreeNode<T>> childNodes = new ConcurrentHashMap<>();

    public TreeNode(String topic) {
        this.topic = topic;
    }

    public boolean add(T t) {
        String[] topics = t.getTopicFilter().split("/");
        return addIndex(t, topics, 0);
    }

    private boolean addTree(T t) {
        return topicSet.add(t);
    }

    private boolean addIndex(T t, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TreeNode<T> treeNode = childNodes.computeIfAbsent(lastTopic, tp -> new TreeNode<>(lastTopic));
        if (index == topics.length - 1) {
            return treeNode.add(t);
        } else {
            return treeNode.addIndex(t, topics, index + 1);
        }
    }

    public List<T> getTopicsByTopic(String topicFilter) {
        String[] topics = topicFilter.split("/");
        return searchTree(topics);
    }


    private List<T> searchTree(String[] topics) {
        LinkedList<T> subscribeTopicList = new LinkedList<>();
        loadTreeSubscribes(this, subscribeTopicList, topics, 0);
        return subscribeTopicList;
    }

    private void loadTreeSubscribes(TreeNode<T> treeNode, LinkedList<T> subscribeTopics, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TreeNode<T> moreTreeNode = treeNode.getChildNodes().get(MORE_SYMBOL);
        if (moreTreeNode != null) {
            subscribeTopics.addAll(moreTreeNode.getTopicSet());
        }
        if (index == topics.length - 1) {
            TreeNode<T> localTreeNode = treeNode.getChildNodes().get(lastTopic);
            if (localTreeNode != null) {
                Set<T> tList = localTreeNode.getTopicSet();
                if (tList != null && tList.size() > 0) {
                    subscribeTopics.addAll(tList);
                }
            }
            localTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (localTreeNode != null) {
                Set<T> tList = localTreeNode.getTopicSet();
                if (tList != null && tList.size() > 0) {
                    subscribeTopics.addAll(tList);
                }
            }

        } else {
            TreeNode<T> oneTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (oneTreeNode != null) {
                loadTreeSubscribes(oneTreeNode, subscribeTopics, topics, index + 1);
            }
            TreeNode<T> node = treeNode.getChildNodes().get(lastTopic);
            if (node != null) {
                loadTreeSubscribes(node, subscribeTopics, topics, index + 1);
            }
        }

    }

    public boolean remove(T t) {
        TreeNode<T> node = this;
        String[] topics = t.getTopicFilter().split("/");
        for (String topic : topics) {
            if (node != null) {
                node = node.getChildNodes().get(topic);
            }
        }
        if (node != null) {
            Set<T> tSet = node.getTopicSet();
            if (tSet != null) {
                return tSet.remove(t);
            }
        }
        return false;
    }

    public Set<T> getTopicSet() {
        return getTreeSubscribesTopic(this);
    }

    private Set<T> getTreeSubscribesTopic(TreeNode<T> node) {
        Set<T> allSubscribeTopics = new HashSet<>();
        allSubscribeTopics.addAll(node.getTopicSet());
        allSubscribeTopics.addAll(node.getChildNodes()
                .values()
                .stream()
                .flatMap(treeNode -> treeNode.getTreeSubscribesTopic(treeNode).stream())
                .collect(Collectors.toSet()));
        return allSubscribeTopics;
    }

}
