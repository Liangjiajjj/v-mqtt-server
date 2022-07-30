package com.iot.mqtt.message.handler;

import com.iot.mqtt.relay.message.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liangjiajun
 */
@Slf4j
public class RelayMessageHandler extends SimpleChannelInboundHandler<RelayBaseMessage> {

    public RelayMessageHandler() {
        super();
    }

    public RelayMessageHandler(boolean autoRelease) {
        super(autoRelease);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RelayBaseMessage msg) {
        try {
            switch (msg.getType()) {
                case auth:
                    handlerConnect(ctx, (RelayAuthMessage) msg);
                    break;
                case auth_ack:
                    handlerConnectAck(ctx, (RelayAuthAckMessage) msg);
                    break;
                case ping:
                    handlerPing(ctx, (RelayPingMessage) msg);
                    break;
                case pong:
                    break;
                case pub:
                    handlerPublish(ctx, (RelayPublishMessage) msg);
                    break;
                default:
            }
        } catch (Exception e) {
            log.error("RelayMessageHandler error !!!", e);
        }
    }


    protected void handlerConnect(ChannelHandlerContext ctx, RelayAuthMessage message) {
    }

    protected void handlerConnectAck(ChannelHandlerContext ctx, RelayAuthAckMessage message) {
    }

    protected void handlerPublish(ChannelHandlerContext ctx, RelayPublishMessage message) {
    }

    protected void handlerPing(ChannelHandlerContext ctx, RelayPingMessage message) {
    }
}
