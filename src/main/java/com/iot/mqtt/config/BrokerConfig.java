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
    @Value("${mqtt.broker.id}")
    private String id;
    @Value("${mqtt.broker.cluster_enabled}")
    private boolean clusterEnabled;

}
