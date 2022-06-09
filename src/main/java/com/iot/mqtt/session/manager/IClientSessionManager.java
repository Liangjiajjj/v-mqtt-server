package com.iot.mqtt.session.manager;

import com.iot.mqtt.session.ClientSession;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.mqtt.MqttEndpoint;

/**
 * @author liangjiajun
 */
public interface IClientSessionManager {
    /**
     * 存储会话
     */
    ClientSession register(String brokerId, MqttEndpoint endpoint, EventExecutor executor);

    /**
     * 设置session失效时间
     */
    void expire(String clientId, int expire);

    /**
     * 获取会话
     */
    ClientSession get(String clientId);

    /**
     * clientId的会话是否存在
     */
    boolean containsKey(String clientId);

    /**
     * 删除会话
     */
    void remove(String clientId);
}
