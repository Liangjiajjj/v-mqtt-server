package com.iot.mqtt.relay.impl;

import com.iot.mqtt.relay.IRelayMessageService;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
/**
 * 单机模式不需要转发
 */
public class NoneRelayMessageService implements IRelayMessageService {

    @Override
    public void relayMessage(String brokerId, String clientId, int messageId, MqttPublishMessage message) {
        throw new RuntimeException("stand-alone not relay message !!!");
    }

}
