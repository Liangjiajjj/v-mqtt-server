package com.iot.mqtt.relay.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.message.handler.RelayMessageHandler;
import com.iot.mqtt.relay.message.*;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import com.iot.mqtt.util.Md5Util;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Resource
    private IClientChannelManager clientChannelManager;

    @Value(value = "rsa.relay_server_password")
    private String username;

    @Value(value = "rsa.relay_server_password")
    private String password;

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
            if (username.equals(message.getUserName()) && password.equals(message.getPassWord())) {
                ctx.writeAndFlush(new RelayAuthAckMessage());
                return;
            }
            ctx.close();
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    protected void handlerPublish(ChannelHandlerContext ctx, RelayPublishMessage message) {
        try {
            String clientId = message.getClientId();
            relayPublishServerExecutor.get(Md5Util.hash(clientId)).execute(() -> relayPublish(clientId, message));
        } catch (Exception e) {
            log.error("handlerPublish error !!! ", e);
            ReferenceCountUtil.release(message);
        }
    }

    private void relayPublish(String clientId, RelayPublishMessage message) {
        ClientChannel clientChannel = clientChannelManager.get(clientId);
        if (Objects.isNull(clientChannel)) {
            log.warn("receive relay message, Channel is null ... clientId:{}   ", clientId);
            ReferenceCountUtil.release(message);
            return;
        }
        try {
            clientChannel.relayPublish(message.getRelayMessage());
        } catch (Throwable throwable) {
            log.error("listener relay error message !!!", throwable);
        } finally {
            ReferenceCountUtil.release(message);
        }
    }
}
