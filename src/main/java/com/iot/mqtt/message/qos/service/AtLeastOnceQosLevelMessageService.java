package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * @author liangjiajun
 * 至少一次
 */
@Slf4j
@Service(value = CommonConstant.AT_LEAST_ONCE + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE)
public class AtLeastOnceQosLevelMessageService extends BaseQosLevelMessageService {

    @Resource
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public void publish(ClientChannel fromChannel, Subscribe subscribe, MqttPublishMessage message, CompletableFuture<Void> future) {
        String clientId = subscribe.getClientId();
        log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", subscribe.getClientId(), subscribe.getTopicFilter(), subscribe.getMqttQoS());
        publish0(clientId, message, future);
        // qos = 1/2 需要保存消息，确保发送到位
        // qos 1 收到 PubAck 证明已经完成这次发送 publish 发送
        dupPublishMessageManager.put(clientId, message);
    }

    @Override
    public void publishReply(ClientChannel channel, Integer messageId) {
        channel.publishAcknowledge(messageId);
    }

}
