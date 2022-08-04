package com.iot.mqtt.filter;

import com.iot.mqtt.subscribe.Subscribe;

import java.util.Set;

/**
 * @author liangjiajun
 */
public interface TopicFilter<T extends BaseTopicBean> {

    /**
     * 获取订阅topic
     *
     * @param topic topic
     * @return {@link Subscribe}
     */
    Set<T> getSet(String topic);


    /**
     * 保存订阅topic
     *
     * @param t {@link T}
     */
    void add(T t);


    /**
     * 保存订阅topic
     *
     * @param t {@link T}
     */
    void remove(T t);


    /**
     * 获取订阅总数
     *
     * @return 总数
     */
    int count();


    /**
     * 获取订所有订阅topic
     *
     * @return {@link Subscribe}
     */
    Set<T> getAllSet();


}
