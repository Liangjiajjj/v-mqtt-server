package com.iot.mqtt.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

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
     * redis是否开启key变化通知
     */
    @Value("${mqtt.redis_key_notify}")
    private Boolean isRedisKeyNotify;

    /**
     * 是否开启批量转发消息
     */
    @Value("${mqtt.is_batch_relay}")
    private Boolean isBatchRelay;

    /**
     * 是否开启批量发消息
     */
    @Value("${mqtt.is_batch_push}")
    private Boolean isBatchPush;

    /**
     * 每次批量发送多少条数据
     */
    @Value("${mqtt.batch_relay_count}")
    private Integer batchRelayCount;

    /**
     * 最大能接受转发消息的延迟(单位：毫秒)
     */
    @Value("${mqtt.max_batch_relay_delay}")
    private Integer maxBatchRelayDelay;

    /**
     * 逻辑线程池
     */
    @Value("${mqtt.work_threads}")
    private Integer workThreads;


    /**
     * 转发消息线程池
     */
    @Value("${mqtt.relay_push_server_threads}")
    private Integer relayPushServerThreads;

    /**
     * 转发消息线程池
     */
    @Value("${mqtt.relay_push_client_threads}")
    private Integer relayPushClientThreads;

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

    // =========================================================== 转发配置 ===========================================================

    /**
     * 推送消息线程池
     */
    @Value("${mqtt.is_open_relay_server}")
    private Boolean isOpenRelayServer;

    /**
     * 推送serverMap
     */
    @Value("#{${mqtt.broker_id_2_url_map}}")
    private Map<String, String> brokerId2UrlMap;

    /**
     * 推送消息线程池
     */
    @Value("${mqtt.push_threads}")
    private Integer pushThreads;

    /**
     * 转发host
     */
    @Value("${mqtt.relay_host}")
    private String relayHost;

    /**
     * 转发端口
     */
    @Value("${mqtt.relay_port}")
    private Integer relayPort;

    /**
     * 转发服务端 boss 线程数
     */
    @Value("${mqtt.relay_server_boss_io_threads}")
    private Integer relayServerBossGroupNThreads = 1;

    /**
     * 转发服务端 worker 线程数
     */
    @Value("${mqtt.relay_server_worker_io_threads}")
    private Integer relayServerWorkerGroupNThreads = 16;

    /**
     * 转发客户端 worker 线程数
     */
    @Value("${mqtt.relay_client_worker_io_threads}")
    private Integer relayClientWorkerGroupNThreads = 16;

}
