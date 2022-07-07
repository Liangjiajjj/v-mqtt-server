package com.iot.mqtt.relay.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.relay.IRelayMessageService;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisRelayMessageService implements IRelayMessageService {

    @Autowired
    private MqttConfig mqttConfig;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private IClientChannelManager clientChannelManager;

    @PostConstruct
    private void init() {
        String selfTopic = RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(mqttConfig.getBrokerId());
        log.info("subscription self topic {} ", selfTopic);
        redissonClient.getTopic(selfTopic).addListener(PublishMessageStore.class, (channel, msg) -> {
            String clientId = msg.getClientId();
            int messageId = msg.getMessageId();
            if (log.isTraceEnabled()) {
                log.trace("receive relay message clientId:{} , messageId:{} , message {}", clientId, messageId, new String(msg.getMessageBytes(), StandardCharsets.UTF_8));
            }
            ClientChannel clientChannel = clientChannelManager.get(clientId);
            if (Objects.isNull(clientChannel)) {
                log.warn("receive relay message, Channel is null ... clientId:{} , messageId:{} ", clientId, msg.getMessageId());
                return;
            }
            MqttPublishMessage publishMessage = msg.toMessage();
            clientChannel.publish(publishMessage.variableHeader().topicName(), publishMessage.payload().array(), publishMessage.fixedHeader().qosLevel(), false, false, messageId);
        });
    }

    @Override
    public void relayMessage(String brokerId, String clientId, int messageId, MqttPublishMessage message) {
        RTopic topic = redissonClient.getTopic(RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(brokerId));
        PublishMessageStore messageStore = PublishMessageStore.fromMessage(clientId, message);
        messageStore.setMessageId(messageId);
        topic.publish(messageStore);
    }

}
