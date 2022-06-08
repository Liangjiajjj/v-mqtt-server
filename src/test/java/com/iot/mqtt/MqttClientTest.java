package com.iot.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.messages.MqttConnAckMessage;
import lombok.SneakyThrows;

public class MqttClientTest {

    @SneakyThrows
    public void test() {
        MqttClient client = MqttClient.create(Vertx.vertx());
        Future<MqttConnAckMessage> future = client.connect(1833, "127.0.0.1");
        client.publishHandler(s -> {
            System.out.println("There are new message in topic: " + s.topicName());
            System.out.println("Content(as string) of the message: " + s.payload().toString());
            System.out.println("QoS: " + s.qosLevel());
        });
        future.onComplete((result -> {
            client.subscribe("rpi2/temp", 0);
            for (int i = 0; i < 10; i++) {
                client.publish("rpi2/temp",
                        Buffer.buffer("hello : " + i),
                        MqttQoS.AT_LEAST_ONCE,
                        false,
                        false);
            }
            client.unsubscribe("rpi2/temp");
        }));
    }

    public static void main(String[] args) {
        MqttClientTest test = new MqttClientTest();
        test.test();
    }
}
