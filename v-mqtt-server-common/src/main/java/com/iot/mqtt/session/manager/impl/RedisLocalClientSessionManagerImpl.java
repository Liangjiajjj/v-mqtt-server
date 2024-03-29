package com.iot.mqtt.session.manager.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.event.SessionRefreshEvent;
import com.iot.mqtt.redis.api.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.LocalClientSession;
import com.iot.mqtt.session.manager.api.IClientSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.DeletedObjectListener;
import org.redisson.api.ExpiredObjectListener;
import org.redisson.api.RBucket;
import org.redisson.api.listener.SetObjectListener;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * redis session
 *
 * @author liangjiajun
 */
@Slf4j
@Service
@Conditional(RedisLocalClientSessionManagerImpl.RedisLocalSessionProperty.class)
public class RedisLocalClientSessionManagerImpl extends RedisBaseServiceImpl<JSONObject> implements IClientSessionManager, RedisBaseService<JSONObject> {

    @Resource
    private MqttConfig mqttConfig;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 本进程的会话对象，需要监听 CLIENT_SESSION_KEY 的变动
     */
    private LoadingCache<String, LocalClientSession> LOCAL_SESSION_MAP;

    @PostConstruct
    private void init() {
        LOCAL_SESSION_MAP = CacheBuilder.newBuilder()
                .maximumSize(100000)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, LocalClientSession>() {
                            @Override
                            public LocalClientSession load(String clientId) {
                                return getLocalClientSession(clientId);
                            }
                        });
    }

    private LocalClientSession getLocalClientSession(String clientId) {
        JSONObject jsonObject = getBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId)).get();
        Optional<LocalClientSession> optional = Optional.ofNullable(jsonObject)
                .map((json) -> new ClientSession().fromJson(json))
                .map(session -> LocalClientSession.builder().clientSession(session).build());
        optional.ifPresent((localClientSession -> initListener(clientId, localClientSession)));
        LocalClientSession localClientSession = optional.orElse(null);
        if (log.isTraceEnabled()) {
            log.trace("load ClientSession {}", jsonObject);
        }
        return localClientSession;
    }

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
        String sessionKey = RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId);
        JSONObject sessionJsonObject = clientSession.toJson();
        setBucket(sessionKey, sessionJsonObject);
        if (expire > 0) {
            expire(clientId, expire);
        }
        LocalClientSession localClientSession = LocalClientSession.builder().clientSession(clientSession).build();
        initListener(sessionKey, localClientSession);
        LOCAL_SESSION_MAP.put(clientId, localClientSession);
        return clientSession;
    }

    public LocalClientSession getLocal(String clientId) {
        try {
            return LOCAL_SESSION_MAP.get(clientId);
        } catch (Exception e) {
            // log.error("local session is null !!! {}", clientId, e);
        }
        return null;
    }

    @Override
    public ClientSession get(String clientId) {
        try {
            return LOCAL_SESSION_MAP.get(clientId).getClientSession();
        } catch (Exception e) {
            // log.error("local session is null !!! {}", clientId, e);
        }
        return null;
    }

    @Override
    public boolean containsKey(String clientId) {
        try {
            LocalClientSession clientSession = LOCAL_SESSION_MAP.get(clientId);
            if (Objects.nonNull(clientSession)) {
                return true;
            }
        } catch (Exception e) {
            // log.error("local session is error !!! {}", clientId);
        }
        return getBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId)).isExists();
    }

    @Override
    public void remove(String clientId) {
        String redisKey = RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId);
        removeBucket(redisKey);
        removeListener(clientId);
        refresh(redisKey);
    }

    @Override
    public void expire(String clientId, int expire) {
        expireBucket(RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId), expire);
    }

    /**
     * 监听redis变动，更新本地内存
     */
    private void initListener(String clientId, LocalClientSession clientSession) {
        String sessionKey = RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId);
        int expiredListenerId = getBucket(sessionKey).addListener((ExpiredObjectListener) this::refresh);
        int setListenerId = getBucket(sessionKey).addListener((SetObjectListener) this::refresh);
        int deletedListenerId = getBucket(sessionKey).addListener((DeletedObjectListener) this::refresh);
        clientSession.setSetListenerId(setListenerId);
        clientSession.setExpiredListenerId(expiredListenerId);
        clientSession.setDeletedListenerId(deletedListenerId);
        if (log.isTraceEnabled()) {
            log.trace("initListener LocalClientSession {}", clientSession);
        }
    }

    /**
     * 删除监听redis变动，更新本地内存
     */
    private void removeListener(String clientId) {
        String redisKey = RedisKeyConstant.CLIENT_SESSION_KEY.getKey(clientId);
        LocalClientSession localClientSession = getLocal(clientId);
        if (Objects.nonNull(localClientSession) && mqttConfig.getBrokerId().equals(localClientSession.getClientSession().getBrokerId())) {
            RBucket<JSONObject> bucket = getBucket(redisKey);
            if (Objects.nonNull(bucket)) {
                bucket.removeListener(localClientSession.getSetListenerId());
                bucket.removeListener(localClientSession.getDeletedListenerId());
                bucket.removeListener(localClientSession.getExpiredListenerId());
                if (log.isTraceEnabled()) {
                    log.trace("removeListener LocalClientSession {}", bucket.get());
                }
            }
        }
    }

    /**
     * 刷新内存
     *
     * @param redisKey
     */
    private void refresh(String redisKey) {
        String clientId = redisKey.split(":")[2];
        LOCAL_SESSION_MAP.invalidate(clientId);
        eventPublisher.publishEvent(SessionRefreshEvent.builder().clientId(clientId).build());
        log.info("Local ClientSession refresh clientId {} ", clientId);
    }

    static class RedisLocalSessionProperty extends AllNestedConditions {

        public RedisLocalSessionProperty() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
        static class ClusterEnabled {
        }

        @ConditionalOnProperty(name = "mqtt.redis_key_notify", havingValue = "true")
        static class RedisKeyNotify {
        }
    }

}
