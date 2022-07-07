package com.iot.mqtt.message.handler.message;

import cn.hutool.core.util.StrUtil;
import com.iot.mqtt.auth.IAuthService;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.ClientChannelImpl;
import com.iot.mqtt.channel.MqttAuth;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.DupPubRelMessage;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.handler.base.BaseMessageHandler;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * 设备连接包
 *
 * @author liangjiajun
 */
@Slf4j
@Service(value = "CONNECT" + CommonConstant.MQTT_MESSAGE_HANDLER)
public class ConnectMessageHandler extends BaseMessageHandler<MqttConnectMessage> {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private IAuthService authService;

    @Autowired
    private ISubscribeManager subscribeManager;

    @Autowired
    private IClientSessionManager clientSessionManager;

    @Autowired
    private IClientChannelManager clientChannelManager;

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Autowired
    private IDupPubRelMessageManager dupPubRelMessageManager;

    /**
     * 业务线程池
     */
    private DefaultEventExecutorGroup sessionExecutors;

    @PostConstruct
    public void init() {
        Integer nThreads = Optional.ofNullable(mqttConfig.getWorkThreads())
                .orElse(Runtime.getRuntime().availableProcessors());
        sessionExecutors = new DefaultEventExecutorGroup(nThreads, new DefaultThreadFactory("SESSION-EXECUTOR"));
    }

    @Override
    public void handle(Channel channel, MqttConnectMessage mqttConnectMessage) {
        ClientChannel clientChannel = new ClientChannelImpl(channel, sessionExecutors.next(), mqttConnectMessage);
        clientChannel.getExecutor().execute(() -> {
            try {
                String clientId = clientChannel.clientIdentifier();
                // clientId为空或null的情况, 这里要求客户端必须提供clientId, 不管cleanSession是否为1, 此处没有参考标准协议实现
                if (StrUtil.isBlank(clientId)) {
                    clientChannel.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                    clientChannel.close();
                    return;
                }
                if (mqttConfig.getPasswordMust()) {
                    // 用户名和密码验证, 这里要求客户端连接时必须提供用户名和密码, 不管是否设置用户名标志和密码标志为1, 此处没有参考标准协议实现
                    MqttAuth auth = clientChannel.auth();
                    String username = auth.getUsername();
                    String password = auth.getPassword();
                    if (!authService.checkValid(username, password)) {
                        clientChannel.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                        return;
                    }
                }
                String brokerId = mqttConfig.getBrokerId();
                // 处理链接与相关信息
                this.handlerConnect(clientChannel);
                // 处理连接心跳包
                int expire = this.handlerIdleState(clientChannel);
                // 保存到 clientChannelManager
                clientChannelManager.put(clientChannel);
                // 注册到 clientSessionManager
                clientSessionManager.register(brokerId, clientChannel, expire);
                channel.attr(AttributeKey.valueOf("clientId")).set(clientId);
                // 建立链接
                clientChannel.accept(false);
                log.info("CONNECT - threadName {}, clientId: {}, cleanSession: {}", Thread.currentThread().getName(), clientId, clientChannel.isCleanSession());
                // 如果cleanSession不为0, 需要重发同一clientId存储的未完成的QoS1和QoS2的DUP消息
                this.handleQosMessage(clientChannel);
            } catch (Throwable throwable) {
                log.error("connect fail !!! clientId {}", clientChannel.clientIdentifier(), throwable);
            } finally {
                ReferenceCountUtil.release(mqttConnectMessage);
            }
        });
    }

    private int handlerIdleState(ClientChannel clientChannel) {
        Channel channel = clientChannel.getChannel();
        int keepAliveTimeSeconds = clientChannel.keepAliveTimeSeconds();
        if (channel.pipeline().names().contains("idle")) {
            channel.pipeline().remove("idle");
        }
        int expire = Math.round(keepAliveTimeSeconds * 1.5f);
        channel.pipeline().addFirst("idle", new IdleStateHandler(0, 0, expire));
        return expire;
    }

    private void handlerConnect(ClientChannel clientChannel) {
        String clientId = clientChannel.clientIdentifier();
        // 如果会话中已存储这个新连接的clientId, 就关闭之前该clientId的连接
        if (clientSessionManager.containsKey(clientId)) {
            // 重新链接，清空所有有关信息
            if (clientChannel.isCleanSession()) {
                clientSessionManager.remove(clientId);
                subscribeManager.removeForClient(clientId);
                dupPubRelMessageManager.removeByClient(clientId);
                dupPublishMessageManager.removeByClient(clientId);
            }
            // 关闭之前的链接
            try {
                Optional.ofNullable(clientChannelManager.get(clientId)).ifPresent(ClientChannel::close);
            } catch (Exception e) {
                log.error("关闭链接异常 clientId :" + clientId, e);
            }
        } else {
            // 如果之前没有链接，清空信息即可
            subscribeManager.removeForClient(clientId);
            dupPubRelMessageManager.removeByClient(clientId);
            dupPublishMessageManager.removeByClient(clientId);
        }
    }

    private void handleQosMessage(ClientChannel clientChannel) {
        // 重新链接
        if (!clientChannel.isCleanSession()) {
            String clientId = clientChannel.clientIdentifier();
            for (MqttPublishMessage mqttPublishMessage : dupPublishMessageManager.get(clientId)) {
                clientChannel.publish(mqttPublishMessage.variableHeader().topicName(), mqttPublishMessage.payload().array(),
                        mqttPublishMessage.fixedHeader().qosLevel(), true, false);
            }
            for (DupPubRelMessage dupPubRelMessage : dupPubRelMessageManager.get(clientId)) {
                clientChannel.publishRelease(dupPubRelMessage.getMessageId());
            }
        }
    }

  /*  private void addMessageHandler(ClientChannel channel) {
        MqttEndpoint endpoint = channel.getEndpoint();
        endpoint.subscribeHandler(new SubscribeMessageHandler(context, channel));
        endpoint.unsubscribeHandler(new UnsubscribeMessageHandler(context, channel));
        endpoint.publishHandler(new PublishMessageHandler(context, channel));
        endpoint.disconnectMessageHandler(new DisconnectMessageHandler(context, channel));
        endpoint.closeHandler(new CloseMessageHandler(context, channel));
        endpoint.pingHandler(new PingHandler(context, channel));
        *//**
     * 假如QoS级别是1(最多一次)，端点需要去处理来自客户端的PUBACK消息，为了收到最后的确认消息。可以使用publishAcknowledgeHandler方法。
     *//*
        endpoint.publishAcknowledgeMessageHandler(new PubAckMessageHandler(cntext, channel));
        *//**
     * 假如QoS级别是2(正好一次)，端点需要去处理来自客户端的PUBREC消息。
     * 可以通过publishReceivedHandler方法来完成该操作。
     * 在该Handler内，端点可以使用publishRelease方法响应PUBREL消息给客户端。最后一步是处理来自客户端的PUBCOMP消息；
     * 可以使用publishCompleteHandler来指定一个handler当收到PUBCOMP消息时候调用。
     *//*
        endpoint.publishReleaseMessageHandler(new PublishPubRelMessageHandler(context, channel));
        endpoint.publishCompletionMessageHandler(new PublishPubCompMessageHandler(context, channel));
    }*/
}
