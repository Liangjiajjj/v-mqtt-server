package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.session.ClientSession;
import io.vertx.mqtt.messages.MqttPubCompMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author liangjiajun
 */
@Slf4j
public class PublishPubCompMessageHandler extends BaseMessageHandler<MqttPubCompMessage> {

    private final IDupPubRelMessageManager dupPubRelMessageManager;

    public PublishPubCompMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.dupPubRelMessageManager = context.getBean(IDupPubRelMessageManager.class);
    }

    @Override
    public void handle(MqttPubCompMessage mqttPubCompMessage) {
        log.debug("PUBCOMP - clientId: {}, messageId: {}", channel.getClientId(), mqttPubCompMessage.messageId());
        dupPubRelMessageManager.remove(channel.getClientId(), mqttPubCompMessage.messageId());
    }
}
