package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

public class PingHandler extends BaseMessageHandler<Void> {

    private final IClientSessionManager clientSessionManager;

    public PingHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        clientSessionManager = context.getBean(IClientSessionManager.class);
    }

    @Override
    public void handle(Void unused) {
        String clientId = channel.getClientId();
        ClientSession clientSession = clientSessionManager.get(clientId);
        if (Objects.nonNull(clientSession)) {
            clientSessionManager.expire(clientId, clientSession.getExpire());
        }
    }
}
