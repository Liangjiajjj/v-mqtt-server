package com.iot.mqtt.message.handler.message;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
@Service(value = "DISCONNECT" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class DisconnectMessageHandler extends BaseMessageHandler<MqttMessage> {

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Autowired
    private IClientChannelManager clientChannelManager;

    @Autowired
    private ISubscribeManager subscribeManager;

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Autowired
    private IDupPubRelMessageManager dupPubRelMessageManager;

    @Override
    public void handle0(ClientChannel channel, MqttMessage mqttMessage) {
        String clientId = channel.clientIdentifier();
        ClientSession clientSession = clientSessionManager.get(clientId);
        if (Objects.nonNull(clientSession) && clientSession.getIsCleanSession()) {
            subscribeManager.removeForClient(clientId);
            dupPublishMessageManager.removeByClient(clientId);
            dupPubRelMessageManager.removeByClient(clientId);
        }
        log.debug("DISCONNECT - clientId: {}, cleanSession: {}", clientId, clientSession.getIsCleanSession());
        clientSessionManager.remove(clientId);
        clientChannelManager.remove(clientId);
        channel.close();
    }
}
