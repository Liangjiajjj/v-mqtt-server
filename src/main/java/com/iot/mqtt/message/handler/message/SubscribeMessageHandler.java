package com.iot.mqtt.message.handler.message;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.subscribe.topic.Subscribe;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private ISubscribeManager subscribeManager;

    @Override
    public void handle0(ClientChannel clientChannel, MqttSubscribeMessage message) {
        String clientId = clientChannel.clientIdentifier();
        List<MqttQoS> grantedQosLevels = new ArrayList<>();
        int messageId = message.variableHeader().messageId();
        List<MqttTopicSubscription> subscriptions = message.payload().topicSubscriptions();
        for (MqttTopicSubscription subscription : subscriptions) {
            MqttQoS mqttQoS = subscription.qualityOfService();
            String topicName = subscription.topicName();
            log.debug("Subscription ClientId {} for {} with QoS  {}", clientId, topicName, mqttQoS);
            grantedQosLevels.add(mqttQoS);
            subscribeManager.add(Subscribe.builder().clientId(clientId).topicFilter(topicName).mqttQoS(mqttQoS.value()).build());
        }
        // 确认订阅请求
        clientChannel.subscribeAcknowledge(messageId, grantedQosLevels);
        // 发布保留消息
        for (MqttTopicSubscription subscription : subscriptions) {
            getQosLevelMessageService(subscription.qualityOfService()).sendRetainMessage(clientChannel, subscription.topicName());
        }
    }
}