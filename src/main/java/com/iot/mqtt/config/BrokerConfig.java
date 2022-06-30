package com.iot.mqtt.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author liangjiajun
 */
@Getter
@Configuration
public class BrokerConfig {

    /**
     * 服务器id
     */
    @Value("${mqtt.broker.id}")
    private String brokerId;

    /**
     * 是否开启集群模式
     */
    @Value("${mqtt.broker.cluster_enabled}")
    private Boolean clusterEnabled;

    /**
     * 是否开启 ssl
     */
    @Value("${mqtt.broker.ssl}")
    private Boolean isSsl;

    /**
     * 是否开启身份校验
     */
    @Value("${mqtt.broker.password_must}")
    private Boolean passwordMust;


}
