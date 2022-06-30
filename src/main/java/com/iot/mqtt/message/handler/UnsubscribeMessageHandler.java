package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 退订topic
 *
 * @author liangjiajun
 */
@Slf4j
public class UnsubscribeMessageHandler extends BaseMessageHandler<MqttUnsubscribeMessage> {

    private final ISubscribeManager subscribeManager;

    public UnsubscribeMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.subscribeManager = context.getBean(ISubscribeManager.class);
    }

    @Override
    public void handle(MqttUnsubscribeMessage message) {
        String clientId = channel.getClientId();
        for (String topicName : message.topics()) {
            log.debug("Unsubscription ClientId {} for {} ", clientId, topicName);
            subscribeManager.remove(topicName, clientId);
        }
        // 确认订阅请求
        channel.unsubscribeAcknowledge(message.messageId());
    }
}