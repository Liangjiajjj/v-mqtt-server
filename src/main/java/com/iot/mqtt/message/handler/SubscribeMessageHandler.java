package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * todo:利用mq更新订阅树！！！
 * 订阅topic
 * 不能以 # 或者 + 开头
 * +：当前层通配符
 * /a/b/+
 * /a/b/c 可以
 * /a/b/d 可以
 * /a/b/f 可以
 * /a/b/c/d 不行
 *
 * #：所有层通配符
 * /a/b/#
 * /a/b/c 可以
 * /a/b/c/d 可以
 * /a/b/c/d/f 可以
 *
 * @author liangjiajun
 */
@Slf4j
public class SubscribeMessageHandler extends BaseMessageHandler<MqttSubscribeMessage> {

    private final ISubscribeManager subscribeManager;

    public SubscribeMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.subscribeManager = context.getBean(ISubscribeManager.class);
    }

    @Override
    public void handle(MqttSubscribeMessage message) {
        List<MqttQoS> grantedQosLevels = new ArrayList<>();
        String clientId = channel.getClientId();
        for (MqttTopicSubscription subscription : message.topicSubscriptions()) {
            MqttQoS mqttQoS = subscription.qualityOfService();
            String topicName = subscription.topicName();
            log.debug("Subscription ClientId {} for {} with QoS  {}", clientId, topicName, mqttQoS);
            grantedQosLevels.add(mqttQoS);
            subscribeManager.put(clientId, new Subscribe(clientId, topicName, mqttQoS));
        }
        // 确认订阅请求
        channel.getEndpoint().subscribeAcknowledge(message.messageId(), grantedQosLevels);
    }

    public static void main(String[] args) {
        TreeMap<String,String> map = new TreeMap<>();
    }
}