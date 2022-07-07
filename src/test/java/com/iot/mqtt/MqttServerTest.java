/*
package com.iot.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class MqttServerTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        MqttServer mqttServer = MqttServer.create(vertx);
        mqttServer.endpointHandler(endpoint -> {
            // shows main connect info
            System.out.println("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession());
            if (endpoint.auth() != null) {
                System.out.println("[username = " + endpoint.auth().getUsername() + ", password = " + endpoint.auth().getPassword() + "]");
            }
            System.out.println("[properties = " + endpoint.connectProperties() + "]");
            if (endpoint.will() != null) {
                System.out.println("[will topic = " + endpoint.will().getWillTopic() + " msg = " + endpoint.will().getWillMessageBytes() +
                        " QoS = " + endpoint.will().getWillQos() + " isRetain = " + endpoint.will().isWillRetain() + "]");
            }
            System.out.println("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]");
            // accept connection from the remote client
            endpoint.accept(false);

            // 收到订阅消息
            endpoint.subscribeHandler(subscribe -> {

                List<MqttQoS> grantedQosLevels = new ArrayList<>();
                for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
                    System.out.println("Subscription for " + s.topicName() + " with QoS " + s.qualityOfService());
                    grantedQosLevels.add(s.qualityOfService());
                }
                // 确认订阅请求
                endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
            });

            // 收到取消订阅消息
            endpoint.unsubscribeHandler(unsubscribe -> {
                for (String t : unsubscribe.topics()) {
                    System.out.println("Unsubscription for " + t);
                }
                // 确认订阅请求
                endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
            });

            // 设备信息
            endpoint.publishHandler(message -> {
                System.out.println("Just received message [" + message.payload().toString(Charset.defaultCharset()) + "] with QoS [" + message.qosLevel() + "]");
                if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                    endpoint.publishAcknowledge(message.messageId());
                } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                    endpoint.publishRelease(message.messageId());
                }
            }).publishReleaseHandler(messageId -> {
                endpoint.publishComplete(messageId);
            });


        }).listen(ar -> {
            if (ar.succeeded()) {
                System.out.println("MQTT server is listening on port " + ar.result().actualPort());
            } else {
                System.out.println("Error on starting the server");
                ar.cause().printStackTrace();
            }
        });
    }

}
*/
