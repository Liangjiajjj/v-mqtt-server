package com.iot.mqtt.redis.impl;

import com.iot.mqtt.redis.RedisBatchInterceptor;
import com.iot.mqtt.redis.RedisBaseService;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author liangjiajun
 */
public class RedisBaseServiceImpl<V> implements RedisBaseService<V> {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void setBucket(String key, V v) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getBucket(key).setAsync(v);
        } else {
            redissonClient.getBucket(key).setAsync(v);
        }
    }

    @Override
    public RBucket<V> getBucket(String key) {
        return redissonClient.getBucket(key);
    }

    @Override
    public void removeBucket(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public void expireBucket(String key, int expire) {
        redissonClient.getBucket(key).expire(expire, TimeUnit.SECONDS);
    }

    @Override
    public void putMap(String mapKey, String key, V v) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getMap(mapKey).putAsync(key, v);
        } else {
            redissonClient.getMap(mapKey).putAsync(key, v);
        }
    }

    @Override
    public void putMap(String mapKey, String key, String str) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getMap(mapKey).putAsync(key, str);
        } else {
            redissonClient.getMap(mapKey).putAsync(key, str);
        }
    }

    @Override
    public RMap<String, V> getMap(String mapKey) {
        return redissonClient.getMap(mapKey);
    }

    @Override
    public void removeMap(String mapKey, String key) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getMap(mapKey).removeAsync(key);
        } else {
            redissonClient.getMap(mapKey).removeAsync(key);
        }
    }

    @Override
    public void removeMap(String mapKey) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getMap(mapKey).deleteAsync();
        } else {
            redissonClient.getMap(mapKey).deleteAsync();
        }
    }

    @Override
    public V getMapValue(String mapKey, String key) {
        return (V) redissonClient.getMap(mapKey).get(key);
    }

    @Override
    public void addSet(String key, V v) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getSet(key).addAsync(v);
        } else {
            redissonClient.getSet(key).addAsync(v);
        }
    }

    @Override
    public void addSet(String key, String str) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getSet(key).addAsync(str);
        } else {
            redissonClient.getSet(key).addAsync(str);
        }
    }

    @Override
    public void removeSet(String key, V v) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getSet(key).removeAsync(v);
        } else {
            redissonClient.getSet(key).removeAsync(v);
        }
    }

    @Override
    public void removeSet(String key, String str) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getSet(key).removeAsync(str);
        } else {
            redissonClient.getSet(key).removeAsync(str);
        }
    }

    @Override
    public RFuture<Set<Object>> getSet(String key) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            return batch.getSet(key).readAllAsync();
        } else {
            return redissonClient.getSet(key).readAllAsync();
        }
    }

    @Override
    public void removeSet(String key) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getSet(key).deleteAsync();
        } else {
            redissonClient.getSet(key).deleteAsync();
        }
    }

    @Override
    public RTopic getTopic(String key) {
        return redissonClient.getTopic(key);
    }

    @Override
    public void publish(String key, Object object) {
        RBatch batch = RedisBatchInterceptor.getThreadLocal().get();
        if (Objects.nonNull(batch)) {
            batch.getTopic(key).publishAsync(object);
        } else {
            redissonClient.getTopic(key).publishAsync(object);
        }
    }

    @Override
    public RKeys getKeys() {
        return redissonClient.getKeys();
    }

    @Override
    public RedissonClient getRedisClient() {
        return redissonClient;
    }
}
