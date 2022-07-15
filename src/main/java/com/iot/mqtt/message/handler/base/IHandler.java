package com.iot.mqtt.message.handler.base;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author liangjiajun
 */
public interface IHandler<E> {

    /**
     * @param channel
     * @param e
     */
    void handle(Channel channel, E e);

    /**
     * 获取从管道clientId
     */
    default String getClientId(Channel channel){
        return (String) channel.attr(AttributeKey.valueOf("clientId")).get();
    }
}
