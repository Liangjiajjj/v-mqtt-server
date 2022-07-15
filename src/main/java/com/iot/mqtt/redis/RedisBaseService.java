package com.iot.mqtt.redis;

import org.redisson.api.*;

import java.util.Set;

/**
 * @author liangjiajun
 */
public interface RedisBaseService<V> {

    void setBucket(String key, V v);

    RBucket<V> getBucket(String key);

    void removeBucket(String key);

    void expireBucket(String key, int expire);

    void putMap(String mapKey, String key, V v);

    void putMap(String mapKey, String key, String str);

    RMap<String, V> getMap(String mapKey);

    void removeMap(String mapKey, String key);

    void removeMap(String mapKey);

    V getMapValue(String mapKey, String key);

    void addSet(String key, V v);

    void addSet(String key, String str);

    void removeSet(String key, V v);

    void removeSet(String key, String str);

    RFuture<Set<Object>> getSet(String key);

    void removeSet(String key);

    RTopic getTopic(String key);

    void publish(String key, Object object);

    RKeys getKeys();

    RedissonClient getRedisClient();

}
