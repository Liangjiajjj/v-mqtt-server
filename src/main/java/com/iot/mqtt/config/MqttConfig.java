package com.iot.mqtt.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author liangjiajun
 */
@Getter
@Configuration
public class MqttConfig {

    /**
     * 服务器id
     */
    @Value("${mqtt.id}")
    private String brokerId;

    /**
     * host
     */
    @Value("${mqtt.host}")
    private String host;

    /**
     * 端口
     */
    @Value("${mqtt.port}")
    private Integer port;

    /**
     * 是否开启ssl
     */
    @Value("${mqtt.ssl}")
    private Boolean ssl;

    /**
     * ssl 密码
     */
    @Value("${mqtt.ssl_password}")
    private String sslPassword;

    /**
     * 是否开启集群模式
     */
    @Value("${mqtt.cluster_enabled}")
    private Boolean clusterEnabled;

    /**
     * 逻辑线程池
     */
    @Value("${mqtt.work_threads}")
    private Integer workThreads;


    /**
     * 是否开启 epoll
     */
    @Value("${mqtt.use_epoll}")
    private Boolean useEpoll;

    /**
     * boss 线程数
     */
    @Value("${mqtt.boss_io_threads}")
    private Integer bossGroupNThreads = 1;

    /**
     * worker 线程数
     */
    @Value("${mqtt.worker_io_threads}")
    private Integer workerGroupNThreads = 16;

    /**
     * 是否开启身份校验
     */
    @Value("${mqtt.password_must}")
    private Boolean passwordMust;

    /**
     * 在线时间
     */
    @Value("${mqtt.keep_alive}")
    private Integer keepAlive;

    /**
     * Sokcet参数, 存放已完成三次握手请求的队列最大长度, 默认511长度
     */
    @Value("${mqtt.so_backlog}")
    private Integer soBacklog = 511;

    /**
     * Socket参数, 是否开启心跳保活机制, 默认开启
     */
    @Value("${mqtt.so_keep_alive}")
    private Boolean soKeepAlive = true;

}
