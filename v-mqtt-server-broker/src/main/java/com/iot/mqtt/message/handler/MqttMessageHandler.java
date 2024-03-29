package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.ClientChannelImpl;
import com.iot.mqtt.channel.manager.api.IClientChannelManager;
import com.iot.mqtt.context.MqttServiceContext;
import com.iot.mqtt.handler.IMessageHandler;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.api.IClientSessionManager;
import com.iot.mqtt.subscribe.api.ISubscribeManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ChannelHandler.Sharable
public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    public MqttMessageHandler() {
        super(false);
    }

    @Resource
    protected MqttServiceContext mqttServiceContext;

    @Resource
    private ISubscribeManager subscribeManager;

    @Resource
    private IClientChannelManager clientChannelManager;

    @Resource
    private IClientSessionManager clientSessionManager;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Attribute<Object> attribute = ctx.channel().attr(AttributeKey.valueOf("clientId"));
        if (Objects.nonNull(attribute)) {
            log.info("channelInactive clientId {} ", attribute.get());
        }
        Optional.of(attribute).map(Attribute::get).map(o -> (String) o)
                .ifPresent(clientId -> {
                    ClientSession clientSession = clientSessionManager.get(clientId);
                    clientChannelManager.remove(clientId);
                    clientSessionManager.remove(clientId);
                    if (Objects.nonNull(clientSession) && clientSession.getIsCleanSession()) {
                        subscribeManager.removeForClient(clientId);
                    }
                });
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage message) throws Exception {
        try {
            rejectFailure(ctx, message);
            // 解析协议
            MqttMessageType messageType = message.fixedHeader().messageType();
            IMessageHandler messageHandler = mqttServiceContext.getMessageHandler(messageType);
            messageHandler.handle(ctx.channel(), message);
        } catch (Exception e) {
            ReferenceCountUtil.release(message);
            log.error("message handler is null type {} ", message.fixedHeader().messageType(), e);
        }
    }

    /**
     * 校验版本号，协议格式出错
     *
     * @param ctx
     * @param message
     */
    public void rejectFailure(ChannelHandlerContext ctx, MqttMessage message) {
        ClientChannel clientChannel = new ClientChannelImpl(ctx.channel());
        if (message.decoderResult().isFailure()) {
            Throwable cause = message.decoderResult().cause();
            if (cause instanceof MqttUnacceptableProtocolVersionException) {
                clientChannel.reject(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            } else if (cause instanceof MqttIdentifierRejectedException) {
                clientChannel.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
            }
            ctx.close();
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                Channel channel = ctx.channel();
                String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
                if (Objects.isNull(clientId)) {
                    return;
                }
                ClientChannel clientChannel = clientChannelManager.get(clientId);
                // 发送遗嘱消息
                if (Objects.nonNull(clientChannel)) {
                    clientChannel.handleClosed();
                }
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            // 远程主机强迫关闭了一个现有的连接的异常
            ctx.close();
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }


}
