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

    @Value("${mqtt.io_thread_count}")
    private Integer ioThreadCount;

    @Value("${mqtt.session_thread_count}")
    private Integer sessionThreadCount;

    @Value("${mqtt.port}")
    private Integer port;

    @Value("${mqtt.ssl}")
    private Boolean ssl;


}
