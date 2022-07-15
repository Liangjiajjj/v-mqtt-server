package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.ClientChannelImpl;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.context.MqttServiceContext;
import com.iot.mqtt.message.handler.base.IHandler;
import com.iot.mqtt.redis.annotation.RedisBatch;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    @Autowired
    protected MqttServiceContext mqttServiceContext;

    @Autowired
    private IClientChannelManager clientChannelManager;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage message) throws Exception {
        try {
            rejectFailure(ctx, message);
            // 解析协议
            MqttMessageType messageType = message.fixedHeader().messageType();
            IHandler messageHandler = mqttServiceContext.getMessageHandler(messageType);
            messageHandler.handle(ctx.channel(), message);
        } catch (Exception e) {
            ReferenceCountUtil.release(message);
            log.error("message handler is null type {} ", message.fixedHeader().messageType());
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
        log.error("exceptionCaught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
