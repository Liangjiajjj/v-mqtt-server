package com.iot.mqtt.channel.manager;

import com.iot.mqtt.channel.ClientChannel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.vertx.mqtt.MqttEndpoint;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientChannelManager implements IClientChannelManager {

    private final Map<String, ClientChannel> clientChannelMap = new ConcurrentHashMap<>();

    @Override
    public ClientChannel put(MqttEndpoint endpoint, EventExecutor executor) {
        String clientId = endpoint.clientIdentifier();
        ClientChannel channel = ClientChannel.builder().clientId(clientId)
                                .endpoint(endpoint).executor(executor).build();
        clientChannelMap.put(clientId, channel);
        return channel;
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
