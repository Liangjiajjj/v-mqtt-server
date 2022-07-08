package com.iot.mqtt.subscribe.topic;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * @author liangjiajun
 */
@Getter
@Setter
public class TreeNode {

    private final String topic;

    private int subscribeTopicNumber;

    private Set<Subscribe> subscribes = new CopyOnWriteArraySet<>();

    private Map<String, TreeNode> childNodes = new ConcurrentHashMap<>();

    public TreeNode(String topic) {
        this.topic = topic;
    }

    private final String ONE_SYMBOL = "+";

    private final String MORE_SYMBOL = "#";

    public boolean addSubscribeTopic(Subscribe subscribe) {
        String[] topics = subscribe.getTopicFilter().split("/");
        return addIndex(subscribe, topics, 0);
    }


    private boolean addTreeSubscribe(Subscribe subscribe) {
        return subscribes.add(subscribe);
    }


    private boolean addIndex(Subscribe subscribe, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TreeNode treeNode = childNodes.computeIfAbsent(lastTopic, tp -> new TreeNode(lastTopic));
        if (index == topics.length - 1) {
            return treeNode.addTreeSubscribe(subscribe);
        } else {
            return treeNode.addIndex(subscribe, topics, index + 1);
        }
    }


    public List<Subscribe> getSubscribeByTopic(String topicFilter) {
        String[] topics = topicFilter.split("/");
        return searchTree(topics);
    }


    private List<Subscribe> searchTree(String[] topics) {
        LinkedList<Subscribe> subscribeTopicList = new LinkedList<>();
        loadTreeSubscribes(this, subscribeTopicList, topics, 0);
        return subscribeTopicList;
    }

    private void loadTreeSubscribes(TreeNode treeNode, LinkedList<Subscribe> subscribeTopics, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TreeNode moreTreeNode = treeNode.getChildNodes().get(MORE_SYMBOL);
        if (moreTreeNode != null) {
            subscribeTopics.addAll(moreTreeNode.getSubscribes());
        }
        if (index == topics.length - 1) {
            TreeNode localTreeNode = treeNode.getChildNodes().get(lastTopic);
            if (localTreeNode != null) {
                Set<Subscribe> subscribes = localTreeNode.getSubscribes();
                if (subscribes != null && subscribes.size() > 0) {
                    subscribeTopics.addAll(subscribes);
                }
            }
            localTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (localTreeNode != null) {
                Set<Subscribe> subscribes = localTreeNode.getSubscribes();
                if (subscribes != null && subscribes.size() > 0) {
                    subscribeTopics.addAll(subscribes);
                }
            }

        } else {
            TreeNode oneTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (oneTreeNode != null) {
                loadTreeSubscribes(oneTreeNode, subscribeTopics, topics, index + 1);
            }
            TreeNode node = treeNode.getChildNodes().get(lastTopic);
            if (node != null) {
                loadTreeSubscribes(node, subscribeTopics, topics, index + 1);
            }
        }

    }

    public boolean removeSubscribeTopic(Subscribe subscribe) {
        TreeNode node = this;
        String[] topics = subscribe.getTopicFilter().split("/");
        for (String topic : topics) {
            if (node != null) {
                node = node.getChildNodes().get(topic);
            }
        }
        if (node != null) {
            Set<Subscribe> subscribeTopics = node.getSubscribes();
            if (subscribeTopics != null) {
                return subscribeTopics.remove(subscribe);
            }
        }
        return false;
    }

    public Set<Subscribe> getAllSubscribesTopic() {
        return getTreeSubscribesTopic(this);
    }

    private Set<Subscribe> getTreeSubscribesTopic(TreeNode node) {
        Set<Subscribe> allSubscribeTopics = new HashSet<>();
        allSubscribeTopics.addAll(node.getSubscribes());
        allSubscribeTopics.addAll(node.getChildNodes()
                .values()
                .stream()
                .flatMap(treeNode -> treeNode.getTreeSubscribesTopic(treeNode).stream())
                .collect(Collectors.toSet()));
        return allSubscribeTopics;
    }

}
