package com.iot.mqtt.message.handler;

import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.vertx.mqtt.messages.MqttDisconnectMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author liangjiajun
 */
@Slf4j
public class DisconnectMessageHandler extends BaseMessageHandler<MqttDisconnectMessage> {

    private IClientSessionManager clientSessionManager;

    private ISubscribeManager subscribeManager;

    private IDupPublishMessageManager dupPublishMessageManager;

    private IDupPubRelMessageManager dupPubRelMessageManager;

    public DisconnectMessageHandler(ApplicationContext context, ClientSession clientSession) {
        super(context, clientSession);
        this.clientSessionManager = context.getBean(IClientSessionManager.class);
        this.subscribeManager = context.getBean(ISubscribeManager.class);
        this.dupPublishMessageManager = context.getBean(IDupPublishMessageManager.class);
        this.dupPubRelMessageManager = context.getBean(IDupPubRelMessageManager.class);
    }

    @Override
    public void handle(MqttDisconnectMessage disconnectMessage) {
        String clientId = clientSession.getClientId();
        if (clientSession.isCleanSession()) {
            subscribeManager.removeForClient(clientId);
            dupPublishMessageManager.removeByClient(clientId);
            dupPubRelMessageManager.removeByClient(clientId);
        }
        log.debug("DISCONNECT - clientId: {}, cleanSession: {}", clientId, clientSession.isCleanSession());
        clientSessionManager.remove(clientId);
        clientSession.getEndpoint().close();
    }
}
