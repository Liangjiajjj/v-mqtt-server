package com.iot.mqtt.relay.configuration;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.relay.connection.RelayConnectionPool;
import com.iot.mqtt.relay.handler.RelayMessageDecoderHandler;
import com.iot.mqtt.relay.handler.RelayMessageEncoderHandler;
import com.iot.mqtt.relay.handler.RelayServerHandler;
import com.iot.mqtt.util.netty.EventLoopUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Executors;

/**
 * @author liangjiajun
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "mqtt.is_open_relay_server", havingValue = "true")
public class RelayServerConfiguration {

    @Resource
    private MqttConfig mqttConfig;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RelayServerHandler relayServerHandler;

    @Value(value = "rsa.relay_server_password")
    private String username;

    @Value(value = "rsa.relay_server_password")
    private String password;

    @PostConstruct
    private void init() {
        // 起一个单独线程检查 relayServer 注册表
        relayServer();
    }

    private void checkRelayServer() {

    }

    /**
     * 转发客户端连接池
     */
    @Bean
    public RelayConnectionPool relayClientConnectionPool() throws Exception {
        log.info("relay client pool is listening .");
        EventLoopGroup clientWorkerGroup = EventLoopUtil.newEventLoopGroup(mqttConfig.getRelayClientWorkerGroupNThreads()
                , new DefaultThreadFactory("RELAY-CLIENT-WORKER-GROUP"));
        return new RelayConnectionPool(username, password, clientWorkerGroup);
    }

    /**
     * 转发服务端
     */
    private void relayServer() {
        EventLoopGroup acceptorGroup = EventLoopUtil.newEventLoopGroup(mqttConfig.getRelayServerBossGroupNThreads(), new DefaultThreadFactory("RELAY-SERVER-BOSS-EXECUTOR"));
        EventLoopGroup workerGroup = EventLoopUtil.newEventLoopGroup(mqttConfig.getRelayServerBossGroupNThreads(), new DefaultThreadFactory("RELAY-SERVER-WORK-EXECUTOR"));
        ServerBootstrap bootstrap = new ServerBootstrap().group(acceptorGroup, workerGroup).channel(mqttConfig.getUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        // Netty心跳机制
                        channelPipeline.addLast("idle", new IdleStateHandler(0, 0, mqttConfig.getKeepAlive()));
                        channelPipeline.addLast("decoder", new RelayMessageDecoderHandler());
                        channelPipeline.addLast("encoder", RelayMessageEncoderHandler.INSTANCE);
                        channelPipeline.addLast("handler", relayServerHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, mqttConfig.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, mqttConfig.getSoKeepAlive())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        ChannelFuture channelFuture;
        if (Strings.isNotBlank(mqttConfig.getRelayHost())) {
            channelFuture = bootstrap.bind(mqttConfig.getRelayHost(), mqttConfig.getRelayPort());
        } else {
            channelFuture = bootstrap.bind(mqttConfig.getRelayPort());
        }
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("relay server is listening success , on port {} .", mqttConfig.getRelayPort());
                // 注册到redis上
            } else {
                log.info("relay server is listening fail , on port {} .", mqttConfig.getRelayPort());
            }
        });
    }

}
