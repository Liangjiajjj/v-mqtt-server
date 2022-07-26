package com.iot.mqtt.message.handler.base;

import io.netty.channel.Channel;

/**
 * @author liangjiajun
 */
public interface IMessageHandler<E> extends IHandler {

    /**
     * @param channel
     * @param e
     */
    void handle(Channel channel, E e);

}
