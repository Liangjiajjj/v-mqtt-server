package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.session.ClientSession;
import io.vertx.mqtt.messages.MqttPubAckMessage;
import org.springframework.context.ApplicationContext;

/**
 * 发送消息的应答包
 * @author liangjiajun
 */
public class PubAckMessageHandler extends BaseMessageHandler<MqttPubAckMessage> {

    private final IDupPublishMessageManager dupPublishMessageManager;

    public PubAckMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.dupPublishMessageManager = context.getBean(IDupPublishMessageManager.class);
    }

    @Override
    public void handle(MqttPubAckMessage mqttPubAckMessage) {
        // 收到应答包，删除重试消息
        dupPublishMessageManager.remove(channel.getClientId(), mqttPubAckMessage.messageId());
    }
}
