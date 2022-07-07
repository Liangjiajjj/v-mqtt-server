package com.iot.mqtt.channel;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.message.handler.base.IHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.EventExecutor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 客户端链接(session与channel隔离)
 */
@Slf4j
@Getter
@Builder
@AllArgsConstructor
public class ClientChannelImpl implements ClientChannel {

    private static final int MAX_MESSAGE_ID = 65535;

    /**
     * 管道
     */
    private final Channel channel;


    /**
     * 业务线程
     */
    private EventExecutor executor;

    /**
     * 关闭处理器
     */
    private IHandler<Void> closeHandler;

    /**
     * id生成器（内存）
     */
    private int messageIdCounter;

    /**
     * 是否联调
     */
    private volatile boolean isConnected;

    /**
     * 是否已经关闭链接
     */
    private volatile boolean isClosed;

    /**
     * clientId
     */
    private final String clientIdentifier;

    /**
     * 认证信息
     */
    private final MqttAuth auth;

    /**
     * 遗嘱消息
     */
    private final MqttWill will;

    /**
     * 是否清空
     */
    private final boolean isCleanSession;

    /**
     * 版本
     */
    private final int protocolVersion;

    /**
     * 心跳时间
     */
    private final int keepAliveTimeoutSeconds;

    /**
     * 配置
     */
    private final MqttProperties connectProperties;

    public ClientChannelImpl(Channel channel) {
        this.channel = channel;
        this.clientIdentifier = null;
        this.auth = null;
        this.will = null;
        this.isCleanSession = false;
        this.protocolVersion = 0;
        this.keepAliveTimeoutSeconds = 0;
        this.connectProperties = null;
    }

    public ClientChannelImpl(Channel channel, EventExecutor executor, MqttConnectMessage msg) {
        // retrieve will information from CONNECT message
        MqttWill will =
                new MqttWill(msg.variableHeader().isWillFlag(),
                        msg.payload().willTopic(),
                        msg.payload().willMessageInBytes() != null ?
                                msg.payload().willMessageInBytes() : null,
                        msg.variableHeader().willQos(),
                        msg.variableHeader().isWillRetain(),
                        msg.payload().willProperties());

        // retrieve authorization information from CONNECT message
        MqttAuth auth = (msg.variableHeader().hasUserName() &&
                msg.variableHeader().hasPassword()) ?
                new MqttAuth(
                        msg.payload().userName(),
                        msg.payload().password()) : null;

        // check if remote MQTT client didn't specify a client-id
        boolean isZeroBytes = (msg.payload().clientIdentifier() == null) ||
                msg.payload().clientIdentifier().isEmpty();

        String clientIdentifier = null;
        // client-id got from payload or auto-generated (according to options)
        if (!isZeroBytes) {
            clientIdentifier = msg.payload().clientIdentifier();
        }

        this.will = will;
        this.auth = auth;
        this.clientIdentifier = clientIdentifier;
        this.isCleanSession = msg.variableHeader().isCleanSession();
        this.protocolVersion = msg.variableHeader().version();
        this.keepAliveTimeoutSeconds = msg.variableHeader().keepAliveTimeSeconds();
        this.connectProperties = msg.variableHeader().properties();
        this.channel = channel;
        this.executor = executor;
    }

    public String getId() {
        return channel.id().asLongText();
    }

    public void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId) {
        publish(topic, payload, qosLevel, isDup, isRetain, messageId, MqttProperties.NO_PROPERTIES);
    }

    @Override
    public void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain) {
        publish(topic, payload, qosLevel, isDup, isRetain, nextMessageId(), MqttProperties.NO_PROPERTIES);
    }


    public void publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId, MqttProperties properties) {
        if (messageId > MAX_MESSAGE_ID || messageId < 0) {
            throw new IllegalArgumentException("messageId must be non-negative integer not larger than " + MAX_MESSAGE_ID);
        }

        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, qosLevel, isRetain, 0);
        MqttPublishVariableHeader variableHeader =
                new MqttPublishVariableHeader(topic, messageId, properties);

        ByteBuf buf = Unpooled.copiedBuffer(payload);

        io.netty.handler.codec.mqtt.MqttMessage publish = MqttMessageFactory.newMessage(fixedHeader, variableHeader, buf);

        this.write(publish);
    }


    @Override
    public ClientChannelImpl pong() {

        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);

        io.netty.handler.codec.mqtt.MqttMessage pingresp = MqttMessageFactory.newMessage(fixedHeader, null, null);

        this.write(pingresp);

        return this;
    }


    public ClientChannelImpl unsubscribeAcknowledge(int unsubscribeMessageId) {
        return unsubscribeAcknowledge(unsubscribeMessageId, MqttProperties.NO_PROPERTIES);
    }

    private ClientChannelImpl unsubscribeAcknowledge(int unsubscribeMessageId, MqttProperties properties) {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader =
                new MqttMessageIdAndPropertiesVariableHeader(unsubscribeMessageId, properties);

        io.netty.handler.codec.mqtt.MqttMessage unsuback = MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);

        this.write(unsuback);

        return this;
    }

    public ClientChannelImpl publishAcknowledge(int publishMessageId) {
        MqttPubAckMessage puback = (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(publishMessageId), null);
        this.write(puback);
        return this;
    }

    public ClientChannelImpl publishRelease(int publishMessageId) {
        return publishRelease(publishMessageId, MqttProperties.NO_PROPERTIES);
    }

    private ClientChannelImpl publishRelease(int publishMessageId, MqttProperties properties) {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader =
                new MqttMessageIdAndPropertiesVariableHeader(publishMessageId, properties);

        io.netty.handler.codec.mqtt.MqttMessage pubrel = MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);

        this.write(pubrel);

        return this;
    }

    @Override
    public ClientChannelImpl publishComplete(int publishMessageId) {
        return publishComplete(publishMessageId, MqttProperties.NO_PROPERTIES);
    }

    private ClientChannelImpl publishComplete(int publishMessageId, MqttProperties properties) {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader =
                new MqttMessageIdAndPropertiesVariableHeader(publishMessageId, properties);

        io.netty.handler.codec.mqtt.MqttMessage pubcomp = MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);

        this.write(pubcomp);
        return this;
    }


    @Override
    public ClientChannelImpl accept() {
        return accept(false);
    }

    @Override
    public ClientChannelImpl accept(boolean sessionPresent) {
        return accept(sessionPresent, MqttProperties.NO_PROPERTIES);
    }

    @Override
    public ClientChannelImpl accept(boolean sessionPresent, MqttProperties properties) {
        synchronized (channel) {
            if (this.isConnected) {
                throw new IllegalStateException("Connection already accepted");
            }

            return this.connack(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent, properties);
        }
    }

    @Override
    public ClientChannelImpl reject(MqttConnectReturnCode returnCode) {
        return reject(returnCode, MqttProperties.NO_PROPERTIES);
    }

    @Override
    public ClientChannelImpl reject(MqttConnectReturnCode returnCode, MqttProperties properties) {
        synchronized (channel) {
            if (returnCode == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                throw new IllegalArgumentException("Need to use the 'accept' method for accepting connection");
            }

            return this.connack(returnCode, false, properties);
        }
    }

    private ClientChannelImpl connack(MqttConnectReturnCode returnCode, boolean sessionPresent, MqttProperties properties) {

        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader variableHeader =
                new MqttConnAckVariableHeader(returnCode, sessionPresent, properties);

        io.netty.handler.codec.mqtt.MqttMessage connack = MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);

        write(connack);

        if (returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
            this.close();
        } else {
            this.isConnected = true;
        }
        return this;
    }


    public ClientChannelImpl subscribeAcknowledge(int subscribeMessageId, List<MqttQoS> grantedQoSLevels) {
        return subscribeAcknowledgeWithCode(subscribeMessageId,
                grantedQoSLevels.stream().mapToInt(MqttQoS::value).toArray(),
                MqttProperties.NO_PROPERTIES);
    }

    private ClientChannelImpl subscribeAcknowledgeWithCode(int subscribeMessageId, int[] reasonCodes, MqttProperties properties) {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader =
                new MqttMessageIdAndPropertiesVariableHeader(subscribeMessageId, properties);

        MqttSubAckPayload payload = new MqttSubAckPayload(reasonCodes);

        io.netty.handler.codec.mqtt.MqttMessage suback = MqttMessageFactory.newMessage(fixedHeader, variableHeader, payload);

        this.write(suback);

        return this;
    }

    public ClientChannelImpl publishReceived(int publishMessageId) {
        return publishReceived(publishMessageId, MqttProperties.NO_PROPERTIES);
    }

    private ClientChannelImpl publishReceived(int publishMessageId, MqttProperties properties) {
        MqttFixedHeader fixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader =
                new MqttMessageIdAndPropertiesVariableHeader(publishMessageId, properties);

        io.netty.handler.codec.mqtt.MqttMessage pubrec = MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);

        this.write(pubrec);

        return this;
    }

    private ChannelFuture write(MqttMessage mqttMessage) {
        synchronized (this.channel) {
            if (mqttMessage.fixedHeader().messageType() != MqttMessageType.CONNACK) {
                this.checkConnected();
            }
            return this.channel.writeAndFlush(mqttMessage);
        }
    }

    private void checkClosed() {
        if (this.isClosed) {
            throw new IllegalStateException("MQTT endpoint is closed");
        }
    }

    private void checkConnected() {
        if (!this.isConnected) {
            throw new IllegalStateException("Connection not accepted yet");
        }
    }

    private void cleanup() {
        if (!this.isClosed) {
            this.isClosed = true;
            this.isConnected = false;
        }
    }

    public void close() {
        synchronized (this.channel) {
            checkClosed();
            this.channel.close();

            this.cleanup();
        }
    }

    @Override
    public String clientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public MqttAuth auth() {
        return auth;
    }

    @Override
    public MqttWill will() {
        return will;
    }

    @Override
    public int protocolVersion() {
        return protocolVersion;
    }

    @Override
    public int keepAliveTimeSeconds() {
        return keepAliveTimeoutSeconds;
    }

    @Override
    public ClientChannel closeHandler(IHandler<Void> handler) {
        synchronized (this.channel) {
            this.checkClosed();
            this.closeHandler = handler;
            return this;
        }
    }

    public void handleClosed() {
        synchronized (this.channel) {
            this.cleanup();
            if (this.closeHandler != null) {
                this.closeHandler.handle(channel, null);
            }
        }
    }

    @Override
    public JSONObject toJson() {
        return null;
    }

    private int nextMessageId() {
        // if 0 or MAX_MESSAGE_ID, it becomes 1 (first valid messageId)
        this.messageIdCounter = ((this.messageIdCounter % MAX_MESSAGE_ID) != 0) ? this.messageIdCounter + 1 : 1;
        return this.messageIdCounter;
    }

}
