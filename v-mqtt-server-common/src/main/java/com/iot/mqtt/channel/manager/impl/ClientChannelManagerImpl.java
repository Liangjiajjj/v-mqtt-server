package com.iot.mqtt.channel.manager.impl;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.api.IClientChannelManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientChannelManagerImpl implements IClientChannelManager {

    private final Map<String, ClientChannel> clientChannelMap = new ConcurrentHashMap<>();

    @Override
    public ClientChannel put(ClientChannel clientChannel) {
        String clientId = clientChannel.clientIdentifier();
        clientChannelMap.put(clientId, clientChannel);
        return clientChannel;
    }

    @Override
    public void expire(String clientId, int expire) {

    }

    @Override
    public ClientChannel get(String clientId) {
        return clientChannelMap.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return clientChannelMap.containsKey(clientId);
    }

    @Override
    public void remove(String clientId) {
        clientChannelMap.remove(clientId);
    }
}
