package com.iot.mqtt.session.manager.impl;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * redis session
 *
 * @author liangjiajun
 */
@Service
@ConditionalOnExpression("${emqtt.cluster_enabled:true}&&${emqtt.redis_key_notify:false}")
public class RedisClientSessionManagerImpl extends RedisBaseServiceImpl<JSONObject> implements IClientSessionManager, RedisBaseService<JSONObject> {

    @Override
    public ClientSession register(String brokerId, ClientChannel clientChannel, int expire) {
        String clientId = clientChannel.clientIdentifier();
        ClientSession clientSession = ClientSession.builder().brokerId(brokerId)
                .expire(expire)
                .clientId(clientId)
                .isCleanSession(clientChannel.isCleanSession())
                .will(clientChannel.will())
                .md5Key(clientChannel.getMd5Key())
                .build();
        setBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId), clientSession.toJson());
        if (expire > 0) {
            expire(clientId, expire);
        }
        return clientSession;
    }

    @Override
    public ClientSession get(String clientId) {
        return Optional.ofNullable(getBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId)).get())
                .map((json) -> new ClientSession().fromJson(json))
                .orElse(null);
    }

    @Override
    public boolean containsKey(String clientId) {
        return getBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId)).isExists();
    }

    @Override
    public void remove(String clientId) {
        removeBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId));
    }

    @Override
    public void expire(String clientId, int expire) {
        expireBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId), expire);
    }

}
