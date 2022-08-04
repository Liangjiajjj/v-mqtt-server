package com.iot.mqtt.dup.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.dup.DupPubRelMessage;
import com.iot.mqtt.dup.api.IDupPubRelMessageManager;
import com.iot.mqtt.redis.api.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * @author liangjiajun
 */
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisDupPubRelMessageManagerImpl extends RedisBaseServiceImpl<DupPubRelMessage> implements IDupPubRelMessageManager, RedisBaseService<DupPubRelMessage> {

    @Override
    public void put(String clientId, DupPubRelMessage publishMessage) {
        int messageId = publishMessage.getMessageId();
        putMap(RedisKeyConstant.DUP_PUBREL_KEY.getKey(clientId), String.valueOf(messageId), publishMessage);
    }

    @Override
    public Collection<DupPubRelMessage> get(String clientId) {
        return getMap(RedisKeyConstant.DUP_PUBREL_KEY.getKey(clientId)).values();
    }

    @Override
    public void remove(String clientId, int messageId) {
        removeMap(RedisKeyConstant.DUP_PUBREL_KEY.getKey(clientId), String.valueOf(messageId));
    }

    @Override
    public void removeByClient(String clientId) {
        removeMap(clientId);
    }

}
