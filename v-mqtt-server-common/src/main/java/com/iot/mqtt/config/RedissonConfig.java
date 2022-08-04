package com.iot.mqtt.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author liangjiajun
 */
@Configuration
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedissonConfig {
    @Bean
    public RedissonClient redisson() throws IOException {
        Config config = Config.fromYAML(RedissonConfig.class.getClassLoader().getResource("config/redisson-config.yml"));
        return Redisson.create(config);
    }
}
