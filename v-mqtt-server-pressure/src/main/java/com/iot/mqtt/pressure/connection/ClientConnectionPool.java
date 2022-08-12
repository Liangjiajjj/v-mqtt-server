package com.iot.mqtt.pressure.connection;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.iot.mqtt.statistics.QpsStatistics;
import com.iot.mqtt.util.netty.EventLoopUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.iot.mqtt.util.netty.ChannelFutures.toCompletableFuture;

@Slf4j
@Component
public class ClientConnectionPool {

    /**
     * 已经失败的客户端id
     */
    @Getter
    private final Set<Integer> failClientIds = new ConcurrentHashSet<>();

    private final Bootstrap bootstrap;

    private final EventLoopGroup ioLoopGroup = EventLoopUtil.newEventLoopGroup(64
            , new DefaultThreadFactory("PRESSURE-IO-CLIENT-WORKER-GROUP"));

    private final EventLoopGroup eventLoopGroup = EventLoopUtil.newEventLoopGroup(64
            , new DefaultThreadFactory("PRESSURE-CLIENT-WORKER-GROUP"));

    /**
     * qps 统计
     */
    public static QpsStatistics qpsStatistics = new QpsStatistics();
    /**
     * 连接池
     */
    protected final ConcurrentMap<Integer, CompletableFuture<ClientConnection>> pool;

    public ClientConnectionPool() {
        pool = new ConcurrentHashMap<>();
        bootstrap = new Bootstrap();
        bootstrap.group(ioLoopGroup);
        bootstrap.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        try {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline channelPipeline = channel.pipeline();
                            // Netty心跳机制
                            channelPipeline.addLast("idle", new IdleStateHandler(0, 0, 60));
                            channelPipeline.addLast("decoder", new MqttDecoder());
                            channelPipeline.addLast("encoder", MqttEncoder.INSTANCE);
                            channelPipeline.addLast("handler", new ClientConnection(failClientIds, eventLoopGroup));
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to create channel initializer", e);
        }
    }

    public CompletableFuture<ClientConnection> getConnection(Integer clientId, InetSocketAddress address) {
        return pool.computeIfAbsent(clientId, k -> createConnection(clientId, address));
    }

    private CompletableFuture<ClientConnection> createConnection(Integer clientId, InetSocketAddress address) {
        final CompletableFuture<ClientConnection> cnxFuture = new CompletableFuture<>();
        toCompletableFuture(bootstrap.connect(address)).whenComplete((channel, throwable) -> {
            if (Objects.nonNull(throwable)) {
                cnxFuture.completeExceptionally(throwable);
                cleanupConnection(clientId);
                return;
            }
            final ClientConnection cnx = (ClientConnection) channel.pipeline().get("handler");
            cnx.setClientId(clientId.toString());
            cnxFuture.complete(cnx);
        });
        return cnxFuture;
    }

    private void cleanupConnection(Integer clientId) {
        pool.remove(clientId);
    }


}
