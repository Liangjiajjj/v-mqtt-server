package com.iot.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;

@RunWith(SpringRunner.class)
@SpringBootTest
class MqttClientTest {

    public static MqttClient client;

    public static String topic = "$123";

    @PostConstruct
    public void before() {
        MqttClient client = MqttClient.create(Vertx.vertx());
        client.connect(1883, "127.0.0.1", s -> {
            System.out.println(s.succeeded());
        });
    }

    @Test
    public static void subscribe() {
        client.publishHandler(s -> {
            System.out.println("There are new message in topic: " + s.topicName());
            System.out.println("Content(as string) of the message: " + s.payload().toString());
            System.out.println("QoS: " + s.qosLevel());
        }).subscribe("rpi2/temp", 2);
    }

    @Test
    public static void unsubscribe() {
        client.publishHandler(s -> {
            System.out.println("There are new message in topic: " + s.topicName());
            System.out.println("Content(as string) of the message: " + s.payload().toString());
            System.out.println("QoS: " + s.qosLevel());
        }).subscribe("rpi2/temp", 2);
    }

    @Test
    public static void publish() {
        client.publish("temperature",
                Buffer.buffer("hello"),
                MqttQoS.AT_LEAST_ONCE,
                false,
                false);
    }

    @Test
    public static void ping() {
        client.pingResponseHandler(s -> {
            //The handler will be called time to time by default
            System.out.println("We have just received PINGRESP packet");
        });
    }
}
