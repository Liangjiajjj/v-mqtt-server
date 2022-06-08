package com.iot.mqtt.message.handler;

import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.session.ClientSession;
import io.vertx.mqtt.messages.MqttPubCompMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@Slf4j
public class PublishPubCompMessageHandler extends BaseMessageHandler<MqttPubCompMessage> {

    private final IDupPubRelMessageManager dupPubRelMessageManager;

    public PublishPubCompMessageHandler(ApplicationContext context, ClientSession clientSession) {
        super(context, clientSession);
        this.dupPubRelMessageManager = context.getBean(IDupPubRelMessageManager.class);
    }

    @Override
    public void handle(MqttPubCompMessage mqttPubCompMessage) {
        log.debug("PUBCOMP - clientId: {}, messageId: {}", clientSession.getClientId(), mqttPubCompMessage.messageId());
        dupPubRelMessageManager.remove(clientSession.getClientId(), mqttPubCompMessage.messageId());
    }
}
