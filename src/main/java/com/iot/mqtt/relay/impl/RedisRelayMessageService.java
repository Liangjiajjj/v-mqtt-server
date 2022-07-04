package com.iot.mqtt.relay.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.BrokerConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.dup.PublishMessageStore;
import com.iot.mqtt.relay.IRelayMessageService;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "true")
public class RedisRelayMessageService implements IRelayMessageService {

    @Autowired
    private BrokerConfig brokerConfig;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private IClientChannelManager clientChannelManager;

    @PostConstruct
    private void init() {
        String selfTopic = RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(brokerConfig.getBrokerId());
        log.info("subscription self topic {} ", selfTopic);
        redissonClient.getTopic(selfTopic).addListener(PublishMessageStore.class, (channel, msg) -> {
            String clientId = msg.getClientId();
            int messageId = msg.getMessageId();
            if (log.isTraceEnabled()) {
                log.trace("receive relay message clientId:{} , messageId:{} ", clientId, messageId);
            }
            ClientChannel clientChannel = clientChannelManager.get(clientId);
            if (Objects.isNull(clientChannel)) {
                log.warn("receive relay message, Channel is null ... clientId:{} , messageId:{} ", clientId, msg.getMessageId());
                return;
            }
            MqttPublishMessage publishMessage = msg.toMessage();
            clientChannel.publish(publishMessage.topicName(), publishMessage.payload(), publishMessage.qosLevel(), false, false, messageId);
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
