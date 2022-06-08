package com.iot.mqtt.session;

import io.netty.util.concurrent.EventExecutor;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttWill;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientSession {

    private String brokerId;

    private MqttEndpoint endpoint;

    private EventExecutor executor;

    public MqttWill getMqttWill() {
        return endpoint.will();
    }

    public boolean isCleanSession() {
        return endpoint.isCleanSession();
    }

    public String getClientId() {
        return endpoint.clientIdentifier();
    }
}
