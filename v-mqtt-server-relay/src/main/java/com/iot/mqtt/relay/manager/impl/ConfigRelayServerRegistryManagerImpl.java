package com.iot.mqtt.relay.manager.impl;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.relay.manager.api.IRelayServerRegistryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * 读取配置注册表管理器
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.is_auto_register", havingValue = "false")
public class ConfigRelayServerRegistryManagerImpl implements IRelayServerRegistryManager {

    @Resource
    private MqttConfig mqttConfig;

    public InetSocketAddress getInetSocketAddress(String brokerId) {
        String url = mqttConfig.getBrokerId2UrlMap().get(brokerId);
        return new InetSocketAddress(url.split(":")[0], Integer.parseInt(url.split(":")[1]));
    }

}
