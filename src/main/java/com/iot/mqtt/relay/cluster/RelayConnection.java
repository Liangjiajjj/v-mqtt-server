package com.iot.mqtt.relay.cluster;

import com.iot.mqtt.message.handler.RelayMessageHandler;
import com.iot.mqtt.relay.message.*;
import com.iot.mqtt.util.MqttEncodeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 转发客户端的链接
 *
 * @author liangjiajun
 */
@Slf4j
public class RelayConnection extends RelayMessageHandler {

    private State state;

    private final int operationTimeoutMs = 10;

    protected String remoteHostName = null;

    protected String proxyToTargetBrokerAddress = null;

    private volatile boolean isActive = true;

    private EventLoopGroup eventLoopGroup;

    private String username;

    private String password;

    private ChannelHandlerContext ctx;

    private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

    public RelayConnection(String username, String password, EventLoopGroup eventLoopGroup) {
        this.username = username;
        this.password = password;
        this.eventLoopGroup = eventLoopGroup;
    }

    public void close() {
        ctx.channel().close();
    }

    public Channel channel() {
        return ctx.channel();
    }

    public ChannelHandlerContext ctx() {
        return ctx;
    }

    /**
     * 是否联通，需要认证才能联通
     *
     * @return
     */
    public CompletableFuture<Void> connectionFuture() {
        return connectionFuture;
    }

    public void setRemoteHostName(String hostName) {
        this.remoteHostName = hostName;
    }

    public void setTargetBroker(InetSocketAddress targetBrokerAddress) {
        this.proxyToTargetBrokerAddress = String.format("%s:%d", targetBrokerAddress.getHostString(),
                targetBrokerAddress.getPort());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
        ctx.writeAndFlush(new RelayAuthMessage(username, password));
        this.eventLoopGroup.schedule(this::checkConnectionTimeout, operationTimeoutMs, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (state != State.Failed) {
            // No need to report stack trace for known exceptions that happen in disconnections
            log.error("[{}] Got exception {}", ctx.channel().remoteAddress(), cause);
            state = State.Failed;
        } else {
            // At default info level, suppress all subsequent exceptions that are thrown when the connection has already
            // failed
            if (log.isDebugEnabled()) {
                log.debug("[{}] Got exception: {}", ctx.channel().remoteAddress(), cause);
            }
        }
        ctx.close();
    }

    @Override
    protected void handlerConnectAck(ChannelHandlerContext ctx, RelayAuthAckMessage message) {
        // 收到回复包，建立链接
        connectionFuture.complete(null);
        state = State.Ready;
        // 心跳包
        this.eventLoopGroup.scheduleAtFixedRate(this::ping, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("{} Disconnected", ctx.channel());
        isActive = false;
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection already closed"));
        }
        connectionFuture.completeExceptionally(new RuntimeException("Disconnected from server at " + ctx.channel().remoteAddress()));
    }

    @Override
    protected void handlerPing(ChannelHandlerContext ctx, RelayPingMessage message) {
        ctx.writeAndFlush(new RelayPongMessage());
    }

    /**
     * 转发包 publish 包
     *
     * @param publishMessage
     */
    public void relayPublish(String clientId, MqttPublishMessage publishMessage) {
        ByteBuf byteBuf = MqttEncodeUtil.encodePublishMessage(ctx, publishMessage);
        try {
            byteBuf.retain();
            RelayPublishMessage relayPublishMessage = new RelayPublishMessage(clientId, byteBuf);
            ctx.write(relayPublishMessage);
        } catch (Exception e) {
            log.error("relayPublish error !!! ", e);
        } finally {
            byteBuf.release();
        }
    }

    public void flush() {
        ctx.flush();
    }

    private void checkConnectionTimeout() {
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection Timeout !!! "));
        }
    }

    private void ping() {
        ctx.writeAndFlush(new RelayPingMessage());
    }

    enum State {
        None, SentConnectFrame, Ready, Failed, Connecting
    }


}
