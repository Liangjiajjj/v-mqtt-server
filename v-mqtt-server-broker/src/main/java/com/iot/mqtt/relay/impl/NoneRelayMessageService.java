package com.iot.mqtt.relay.impl;

import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.relay.api.IRelayMessageService;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
/**
 * @author liangjiajun
 * 单机模式不需要转发
 */
public class NoneRelayMessageService implements IRelayMessageService {

    @Override
    public void relayMessage(ClientSession clientSession, int messageId, MqttPublishMessage message, CompletableFuture<Void> future) {
        throw new RuntimeException("stand-alone not relay message !!!");
    }

    @Override
    public void batchPublish(ClientSession clientSession, PublishMessageStore publishMessage) {
        throw new RuntimeException("stand-alone not relay message !!!");
    }

    @Override
    public void batchPublish0() {
        throw new RuntimeException("stand-alone not relay message !!!");
    }


}
