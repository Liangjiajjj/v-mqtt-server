/*
package com.iot.mqtt.server;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.message.handler.message.ConnectMessageHandler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

*/
/**
 * @author liangjiajun
 *//*

@Slf4j
@Configuration
public class MqttServerConfiguration1 {

    @Autowired
    private MqttConfig mqttConfig;
    @Autowired
    private ConnectMessageHandler connectMessageHandler;

    @Bean
    public void server() {
        VertxOptions vertxOptions = new VertxOptions().setEventLoopPoolSize(mqttConfig.getIoThreadCount());
        MqttServerOptions options = new MqttServerOptions()
                .setPort(mqttConfig.getPort())
                .setSsl(mqttConfig.getSsl());
        MqttServer mqttServer = MqttServer.create(Vertx.vertx(vertxOptions), options);
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
*/
