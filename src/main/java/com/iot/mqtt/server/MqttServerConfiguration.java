package com.iot.mqtt.server;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.message.handler.AutoFlushHandler;
import com.iot.mqtt.message.handler.MqttMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * @author liangjiajun
 */
@Slf4j
@Configuration
public class MqttServerConfiguration {

    @Autowired
    private MqttConfig mqttConfig;
    @Autowired
    private MqttMessageHandler mqttMessageHandler;
    /**
     * ssl
     */
    private SslContext sslContext;
    /**
     * boss 线程池
     */
    private EventLoopGroup bossGroup;
    /**
     * work 线程池
     */
    private EventLoopGroup workerGroup;

    @Bean
    public void start() throws Exception {
        log.info("MQTT server is listening on port {} ", mqttConfig.getPort());
        Integer bossGroupNThreads = mqttConfig.getBossGroupNThreads();
        Integer workerGroupNThreads = mqttConfig.getWorkerGroupNThreads();
        bossGroup = mqttConfig.getUseEpoll() ? new EpollEventLoopGroup(bossGroupNThreads) : new NioEventLoopGroup(bossGroupNThreads);
        workerGroup = mqttConfig.getUseEpoll() ? new EpollEventLoopGroup(workerGroupNThreads) : new NioEventLoopGroup(workerGroupNThreads);
        if (mqttConfig.getSsl()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("keystore/server.pfx");
            keyStore.load(inputStream, mqttConfig.getSslPassword().toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, mqttConfig.getSslPassword().toCharArray());
            sslContext = SslContextBuilder.forServer(kmf).build();
        }
        mqttServer();
    }

    private void mqttServer() {
        ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup).channel(mqttConfig.getUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        // Netty心跳机制
                        channelPipeline.addLast("idle", new IdleStateHandler(0, 0, mqttConfig.getKeepAlive()));
                        // Netty提供的SSL处理
                        if (mqttConfig.getSsl()) {
                            SSLEngine sslEngine = sslContext.newEngine(channel.alloc());
                            sslEngine.setUseClientMode(false);        // 服务端模式
                            sslEngine.setNeedClientAuth(false);        // 不需要验证客户端
                            channelPipeline.addLast("ssl", new SslHandler(sslEngine));
                        }
                        channelPipeline.addLast("autoflush", new AutoFlushHandler(1, TimeUnit.SECONDS));
                        channelPipeline.addLast("decoder", new MqttDecoder());
                        channelPipeline.addLast("encoder", MqttEncoder.INSTANCE);
                        channelPipeline.addLast("broker", mqttMessageHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, mqttConfig.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, mqttConfig.getSoKeepAlive())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        if (Strings.isNotBlank(mqttConfig.getHost())) {
            bootstrap.bind(mqttConfig.getHost(), mqttConfig.getPort());
        } else {
            bootstrap.bind(mqttConfig.getPort());
        }
    }

}
