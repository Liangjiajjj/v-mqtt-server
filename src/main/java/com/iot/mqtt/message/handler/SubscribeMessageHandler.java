package com.iot.mqtt.message.handler;

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

/**
 * 订阅topic
 *
 * @author liangjiajun
 */
@Slf4j
public class SubscribeMessageHandler extends BaseMessageHandler<MqttSubscribeMessage> {

    private final ISubscribeManager subscribeManager;

    public SubscribeMessageHandler(ApplicationContext context, ClientSession clientSession) {
        super(context, clientSession);
        this.subscribeManager = context.getBean(ISubscribeManager.class);
    }

    @Override
    public void handle(MqttSubscribeMessage message) {
        List<MqttQoS> grantedQosLevels = new ArrayList<>();
        String clientId = clientSession.getClientId();
        for (MqttTopicSubscription subscription : message.topicSubscriptions()) {
            MqttQoS mqttQoS = subscription.qualityOfService();
            String topicName = subscription.topicName();
            if (log.isTraceEnabled()) {
                log.trace("Subscription ClientId {} for {} with QoS  {}", clientId, topicName, mqttQoS);
            }
            grantedQosLevels.add(mqttQoS);
            subscribeManager.put(clientId, new Subscribe(clientId, topicName, mqttQoS));
        }
        // 确认订阅请求
        clientSession.getEndpoint().subscribeAcknowledge(message.messageId(), grantedQosLevels);
    }
}