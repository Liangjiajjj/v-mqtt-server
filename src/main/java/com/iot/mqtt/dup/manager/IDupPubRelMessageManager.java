package com.iot.mqtt.dup.manager;

import com.iot.mqtt.dup.DupPubRelMessage;

import java.util.Collection;

/**
 * PUBREL重发消息存储服务接口, 当QoS=2时存在该重发机制
 * @author liangjiajun
 */
public interface IDupPubRelMessageManager {

    /**
     * 存储消息
     */
    void put(String clientId, DupPubRelMessage publishMessage);

    /**
     * 获取消息集合
     */
    Collection<DupPubRelMessage> get(String clientId);

    /**
     * 删除消息
     */
    void remove(String clientId, int messageId);

    /**
     * 删除消息
     */
    void removeByClient(String clientId);

}
