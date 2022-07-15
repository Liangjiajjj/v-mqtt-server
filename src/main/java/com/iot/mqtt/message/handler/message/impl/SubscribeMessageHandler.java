package com.iot.mqtt.message.handler.message.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 利用mq更新订阅树！！！
 * 订阅topic
 * 不能以 # 或者 + 开头
 * +：当前层通配符
 * /a/b/+
 * /a/b/c 可以
 * /a/b/d 可以
 * /a/b/f 可以
 * /a/b/c/d 不行
 * <p>
 * #：所有层通配符
 * /a/b/#
 * /a/b/c 可以
 * /a/b/c/d 可以
 * /a/b/c/d/f 可以
 *
 * @author liangjiajun
 */
@Slf4j
@Service(value = "SUBSCRIBE" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class SubscribeMessageHandler extends BaseMessageHandler<MqttSubscribeMessage> {

    @Override
    public void handle0(ClientChannel clientChannel, MqttSubscribeMessage message) {
        String clientId = clientChannel.clientIdentifier();
        int messageId = message.variableHeader().messageId();
        subscribeManager.addSubscriptions(clientId, message);
        List<MqttQoS> grantedQosLevels = message.payload().topicSubscriptions().stream().map(MqttTopicSubscription::qualityOfService).collect(Collectors.toList());
        // 确认订阅请求
        clientChannel.subscribeAcknowledge(messageId, grantedQosLevels);
        // 发布保留消息
        for (MqttTopicSubscription subscription : message.payload().topicSubscriptions()) {
            getQosLevelMessageService(subscription.qualityOfService()).sendRetainMessage(clientChannel,
                    subscription.topicName(), subscription.qualityOfService());
        }
    }

}