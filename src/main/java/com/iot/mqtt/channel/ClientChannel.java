package com.iot.mqtt.channel;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.message.handler.base.IHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 客户端链接(session与channel隔离)
 */

public interface ClientChannel {

    ClientChannel accept();

    ClientChannel accept(boolean sessionPresent);

    ClientChannel accept(boolean sessionPresent, MqttProperties properties);

    ClientChannel reject(MqttConnectReturnCode returnCode);

    ClientChannel reject(MqttConnectReturnCode returnCode, MqttProperties properties);

    ClientChannel subscribeAcknowledge(int subscribeMessageId, List<MqttQoS> grantedQoSLevels);

    ClientChannel unsubscribeAcknowledge(int unsubscribeMessageId);

    ClientChannel publishAcknowledge(int publishMessageId);

    ClientChannel publishReceived(int publishMessageId);

    ClientChannel publishRelease(int publishMessageId);

    ClientChannel publishComplete(int publishMessageId);

    void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain);

    void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId);

    void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId, MqttProperties properties);

    ClientChannel pong();

    // ClientChannel disconnect(MqttDisconnectReasonCode code, MqttProperties properties);

    Executor getExecutor();

    void close();

    String clientIdentifier();

    MqttAuth auth();

    MqttWill will();

    int protocolVersion();

    boolean isCleanSession();

    int keepAliveTimeSeconds();

    ClientChannel closeHandler(IHandler<Void> closeHandler);

    Channel getChannel();

    void handleClosed();

    Long getMd5Key();
}
