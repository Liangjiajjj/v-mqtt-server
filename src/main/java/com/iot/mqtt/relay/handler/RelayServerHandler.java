package com.iot.mqtt.relay.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.message.handler.RelayMessageHandler;
import com.iot.mqtt.messageid.service.IMessageIdService;
import com.iot.mqtt.relay.message.*;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import com.iot.mqtt.util.Md5Util;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ChannelHandler.Sharable
public class RelayServerHandler extends RelayMessageHandler {

    public RelayServerHandler() {
        super(false);
    }

    @Resource(name = "RELAY-PUBLISH-SERVER-EXECUTOR")
    private MqttEventExecuteGroup relayPublishServerExecutor;

    @Autowired
    private IClientChannelManager clientChannelManager;

    @Override
    protected void handlerPing(ChannelHandlerContext ctx, RelayPingMessage message) {
        try {
            ctx.writeAndFlush(new RelayPongMessage());
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    protected void handlerConnect(ChannelHandlerContext ctx, RelayAuthMessage message) {
        try {
            // todo:判断是否密码正确
            ctx.writeAndFlush(new RelayAuthAckMessage());
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    protected void handlerPublish(ChannelHandlerContext ctx, RelayPublishMessage message) {
        try {
            String clientId = message.getClientId();
            relayPublishServerExecutor.get(Md5Util.hash(clientId)).execute(() -> {
                ClientChannel clientChannel = clientChannelManager.get(clientId);
                if (Objects.isNull(clientChannel)) {
                    log.warn("receive relay message, Channel is null ... clientId:{}   ", clientId);
                    return;
                }
                try {
                    clientChannel.relayPublish(message.getRelayMessage());
                } catch (Throwable throwable) {
                    log.error("listener relay error message !!!", throwable);
                } finally {
                    ReferenceCountUtil.release(message);
                }
            });
        } catch (Exception e) {
            log.error("handlerPublish error !!! ", e);
            ReferenceCountUtil.release(message);
        }
    }
}
