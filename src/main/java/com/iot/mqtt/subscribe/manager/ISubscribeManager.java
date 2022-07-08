package com.iot.mqtt.subscribe.manager;

import com.iot.mqtt.subscribe.topic.Subscribe;

import java.util.Collection;

/**
 * @author liangjiajun
 */
public interface ISubscribeManager {

    /**
     * 存储订阅
     */
    void add(Subscribe subscribe);

    /**
     * 删除订阅
     */
    void remove(Subscribe subscribe);

    /**
     * 删除clientId的订阅
     */
    void removeForClient(String clientId);

    /**
     * 获取订阅存储集
     */
    Collection<Subscribe> search(String topic);
}
