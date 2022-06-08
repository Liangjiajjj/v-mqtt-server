package com.iot.mqtt.subscribe.manager;

import com.iot.mqtt.subscribe.Subscribe;

import java.util.Collection;

public interface ISubscribeManager {

    /**
     * 存储订阅
     */
    void put(String topicFilter, Subscribe subscribeStore);

    /**
     * 删除订阅
     */
    void remove(String topicFilter, String clientId);

    /**
     * 删除clientId的订阅
     */
    void removeForClient(String clientId);

    /**
     * 获取订阅存储集
     */
    Collection<Subscribe> search(String topic);
}
