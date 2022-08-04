package com.iot.mqtt.relay.manager.impl;

import com.google.common.base.Strings;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.relay.manager.api.IRelayServerRegistryManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 转发服务端注册表管理器
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.is_auto_register", havingValue = "true")
public class RedisRelayServerRegistryManagerImpl implements IRelayServerRegistryManager {

    /**
     * 获取本地ip工具类
     */
    private final InetUtils inetUtils = new InetUtils(new InetUtilsProperties());

    @Resource
    private MqttConfig mqttConfig;

    @Resource
    private RedissonClient redissonClient;

    /**
     * brokerId -> InetSocketAddress
     */
    private final Map<String, InetSocketAddress> relayServerRegistry = new HashMap<>();

    @PostConstruct
    private void init() {
        /**
         * 刷新注册表
         */
        checkRelayServerRegistry();
        /**
         * 每30秒刷新一下注册表
         */
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkRelayServerRegistry, 0, 30, TimeUnit.SECONDS);
    }

    private void checkRelayServerRegistry() {
        try {
            registerRelayServerRegistry();
            String registryKey = RedisKeyConstant.RELAY_SERVER_REGISTRY.getKey("*");
            redissonClient.getKeys().getKeysByPattern(registryKey).forEach((key) -> {
                String address = (String) redissonClient.getBucket(key).get();
                relayServerRegistry.put(key, new InetSocketAddress(address.split(":")[0], Integer.parseInt(address.split(":")[1])));
            });
        } catch (Exception e) {
            log.error("checkRelayServerRegistry error !!! ", e);
        }
    }

    private void registerRelayServerRegistry() {
        String host = mqttConfig.getRelayHost();
        if (Strings.isNullOrEmpty(host)) {
            // 默认找内网ip
            host = inetUtils.findFirstNonLoopbackHostInfo().getHostname();
        }
        redissonClient.getBucket(RedisKeyConstant.RELAY_SERVER_REGISTRY
                .getKey(mqttConfig.getBrokerId()))
                .set(host + ":" + mqttConfig.getRelayPort(), 60, TimeUnit.SECONDS);
    }

    public InetSocketAddress getInetSocketAddress(String brokerId) {
        String registryKey = RedisKeyConstant.RELAY_SERVER_REGISTRY.getKey(brokerId);
        return relayServerRegistry.get(registryKey);
    }

}
