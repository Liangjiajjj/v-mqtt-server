package com.iot.mqtt.relay.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.dup.PublishMessageStore;
import com.iot.mqtt.redis.RedisBaseService;
import com.iot.mqtt.redis.impl.RedisBaseServiceImpl;
import com.iot.mqtt.relay.IRelayMessageService;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisRelayMessageServiceImpl extends RedisBaseServiceImpl<PublishMessageStore> implements IRelayMessageService, RedisBaseService<PublishMessageStore> {

    @Autowired
    private MqttConfig mqttConfig;
    @Resource(name = "PUBLISH-EXECUTOR")
    private MqttEventExecuteGroup mqttEventExecuteGroup;
    @Autowired
    private IClientChannelManager clientChannelManager;

    @PostConstruct
    private void init() {
        String selfTopic = RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(mqttConfig.getBrokerId());
        log.info("subscription self topic {} ", selfTopic);
        getTopic(selfTopic).addListener(PublishMessageStore.class, (channel, msg) -> {
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
            mqttEventExecuteGroup.get(clientChannel.getMd5Key()).execute(() -> {
                clientChannel.publish(publishMessage.variableHeader().topicName(),
                        publishMessage.payload().array(), publishMessage.fixedHeader().qosLevel(),
                        false, false, messageId);
            });
        });
    }

    @Override
    public void relayMessage(String brokerId, String clientId, int messageId, MqttPublishMessage message) {
        // RTopic topic = redissonClient.getTopic(RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(brokerId));
        // topic.publishAsync(messageStore);
        PublishMessageStore messageStore = PublishMessageStore.fromMessage(clientId, message);
        messageStore.setMessageId(messageId);
        publish(RedisKeyConstant.RELAY_MESSAGE_TOPIC.getKey(brokerId), messageStore);
    }

}
