package com.iot.mqtt.pressure.connection;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.ClientChannelImpl;
import com.iot.mqtt.info.MqttAuth;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ClientConnection extends SimpleChannelInboundHandler<MqttMessage> {

    private static AtomicInteger mqttClientId = new AtomicInteger();

    private static AtomicInteger messageId = new AtomicInteger();

    @Setter
    @Getter
    private String clientId;

    private final EventLoop eventLoop;

    private ClientChannel channel;

    private ChannelHandlerContext ctx;

    private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

    public ClientConnection(EventLoopGroup eventLoopGroup) {
        this.eventLoop = eventLoopGroup.next();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        new ClientChannelImpl(ctx.channel()).connect(clientId, new MqttAuth("", ""));
        ctx.channel().attr(AttributeKey.valueOf("clientId")).set(clientId);
        this.eventLoop.schedule(this::checkConnectionTimeout, 30, TimeUnit.SECONDS);
        log.info("{} {} connect active !!! ", ctx.channel(), clientId);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("{} disconnected", ctx.channel());
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection already closed"));
            return;
        }
        connectionFuture.completeExceptionally(new RuntimeException("Disconnected from server at " + ctx.channel().remoteAddress()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        try {
            MqttMessageType type = msg.fixedHeader().messageType();
            switch (type) {
                case CONNACK:
                    handlerConnectAck(ctx, (MqttConnAckMessage) msg);
                    break;
                case PUBLISH:
                    handlerPublish(ctx, (MqttPublishMessage) msg);
                    break;
                default:
            }
        } catch (Exception e) {
            log.error("client read error !!! ", e);
        }
    }

    private void handlerPublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
        long startTime = msg.payload().readLong();
        long costTime = System.currentTimeMillis() - startTime;
        eventLoop.execute(() -> {
            ClientConnectionPool.qpsStatistics.cost(costTime);
        });
    }

    private void handlerConnectAck(ChannelHandlerContext ctx, MqttConnAckMessage msg) {
        log.info("{} {} connect succeed !!! ", ctx.channel(), clientId);
        this.ctx = ctx;
        this.channel = new ClientChannelImpl(clientId, ctx.channel());
        eventLoop.scheduleAtFixedRate(this::ping, 0, 30, TimeUnit.SECONDS);
        // 收到回复包，建立链接
        connectionFuture.complete(null);
    }

    public CompletableFuture<Void> connectionFuture() {
        return connectionFuture;
    }

    public void ping() {
        channel.ping();
    }

    public void subscribe(String topic) {
        channel.subscribe(getNextMessageId(), MqttQoS.AT_LEAST_ONCE, topic);
    }

    /**
     * 一秒一次
     *
     * @param topic
     */
    public void publish(String topic) {
        eventLoop.scheduleAtFixedRate(() -> publish0(topic), 0, 1, TimeUnit.SECONDS);
    }

    private void publish0(String topic) {
        ByteBuf payLoad = ctx.alloc().buffer(8).writeLong(System.currentTimeMillis());
        try {
            payLoad.retain();
            channel.publish(topic,
                    payLoad, MqttQoS.AT_MOST_ONCE,
                    false, false, getNextMessageId());
            channel.flush();
        } catch (Exception e) {
            log.error("publish error !!!", e);
        } finally {
            payLoad.release();
        }
    }

    public int getNextMessageId() {
        try {
            while (true) {
                int nextMsgId = messageId.incrementAndGet() % 65536;
                if (nextMsgId > 0) {
                    return nextMsgId;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    private void checkConnectionTimeout() {
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection Timeout !!! "));
        }
    }

    public ClientChannel getChannel() {
        return channel;
    }
}
