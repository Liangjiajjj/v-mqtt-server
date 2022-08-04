package com.iot.mqtt.handler;

import com.iot.mqtt.handler.IHandler;
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
