package com.iot.mqtt.channel.manager;

import com.iot.mqtt.channel.ClientChannel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.mqtt.MqttEndpoint;

public interface IClientChannelManager {

    /**
     * 存储会话
     */
    ClientChannel put(MqttEndpoint endpoint, EventExecutor executor);

    /**
     * 设置session失效时间
     */
    void expire(String clientId, int expire);

    /**
     * 获取会话
     */
    ClientChannel get(String clientId);

    /**
     * clientId的会话是否存在
     */
    boolean containsKey(String clientId);

    /**
     * 删除会话
     */
    void remove(String clientId);
}
