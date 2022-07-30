package com.iot.mqtt.relay.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * @author liangjiajun
 */
@ChannelHandler.Sharable
public class RelayPublishMessageHandler extends MessageToMessageEncoder<ByteBuf> {

    public static final RelayPublishMessageHandler INSTANCE = new RelayPublishMessageHandler();

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {


    }
}
