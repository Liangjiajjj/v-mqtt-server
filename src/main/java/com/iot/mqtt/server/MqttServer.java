package com.iot.mqtt.server;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.message.handler.ConnectMessageHandler;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liangjiajun
 */
@Slf4j
@Configuration
public class MqttServer {

    @Autowired
    private MqttConfig mqttConfig;
    @Autowired
    private ConnectMessageHandler connectMessageHandler;

    @Bean
    public void server() {
        MqttServerOptions options = new MqttServerOptions()
                .setPort(mqttConfig.getPort())
                .setSsl(mqttConfig.getSsl());
        io.vertx.mqtt.MqttServer mqttServer = io.vertx.mqtt.MqttServer.create(Vertx.vertx(), options);
        mqttServer.endpointHandler(connectMessageHandler).listen((result -> {
            if (result.succeeded()) {
                log.info("MQTT server is listening on port {} ", result.result().actualPort());
            } else {
                log.error("Error on starting the server");
                result.cause().printStackTrace();
            }
        }));
    }

}
