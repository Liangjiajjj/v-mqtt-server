package com.iot.mqtt.message.handler;

import com.iot.mqtt.auth.IAuthService;
import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.config.BrokerConfig;
import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.message.dup.DupPubRelMessage;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.messages.MqttPublishMessage;
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
@Service
public class ConnectMessageHandler implements Handler<MqttEndpoint> {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private BrokerConfig brokerConfig;

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
        Integer nThreads = Optional.ofNullable(mqttConfig.getSessionThreadCount())
                .orElse(Runtime.getRuntime().availableProcessors());
        sessionExecutors = new DefaultEventExecutorGroup(nThreads, new DefaultThreadFactory("SESSION-EXECUTOR"));
    }

    @Override
    public void handle(MqttEndpoint endpoint) {
        if (brokerConfig.getPasswordMust()) {
            String username = endpoint.auth().getUsername();
            String password = endpoint.auth().getPassword();
            if (!authService.checkValid(username, password)) {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                return;
            }
        }
        // 建立链接
        endpoint.accept(false);
        String brokerId = brokerConfig.getBrokerId();
        String clientId = endpoint.clientIdentifier();
        // 处理链接与相关信息
        this.handlerConnect(endpoint, clientId);
        log.debug("CONNECT - clientId: {}, cleanSession: {}", endpoint.clientIdentifier(), endpoint.isCleanSession());
        // 如果cleanSession不为0, 需要重发同一clientId存储的未完成的QoS1和QoS2的DUP消息
        this.handleQosMessage(endpoint);
        // 保存到 clientChannelManager
        ClientChannel channel = clientChannelManager.put(endpoint, sessionExecutors.next());
        // 注册到 clientSessionManager
        clientSessionManager.register(brokerId, endpoint);
        // 添加该链接的消息处理器
        this.addMessageHandler(channel);
    }

    private void handlerConnect(MqttEndpoint endpoint, String clientId) {
        // 如果会话中已存储这个新连接的clientId, 就关闭之前该clientId的连接
        if (clientSessionManager.containsKey(clientId)) {
            // 重新链接，清空所有有关信息
            if (endpoint.isCleanSession()) {
                clientSessionManager.remove(clientId);
                subscribeManager.removeForClient(clientId);
                dupPubRelMessageManager.removeByClient(clientId);
                dupPublishMessageManager.removeByClient(clientId);
            }
            // 关闭之前的链接
            try {
                Optional.of(clientChannelManager.get(clientId)).ifPresent(ClientChannel::close);
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

    private void handleQosMessage(MqttEndpoint endpoint) {
        // 重新链接
        if (!endpoint.isCleanSession()) {
            String clientId = endpoint.clientIdentifier();
            for (MqttPublishMessage mqttPublishMessage : dupPublishMessageManager.get(clientId)) {
                endpoint.publish(mqttPublishMessage.topicName(), mqttPublishMessage.payload(),
                        mqttPublishMessage.qosLevel(), true, false);
            }
            for (DupPubRelMessage dupPubRelMessage : dupPubRelMessageManager.get(clientId)) {
                endpoint.publishRelease(dupPubRelMessage.getMessageId());
            }
        }
    }

    private void addMessageHandler(ClientChannel channel) {
        MqttEndpoint endpoint = channel.getEndpoint();
        endpoint.subscribeHandler(new SubscribeMessageHandler(context, channel));
        endpoint.unsubscribeHandler(new UnsubscribeMessageHandler(context, channel));
        endpoint.publishHandler(new PublishMessageHandler(context, channel));
        endpoint.disconnectMessageHandler(new DisconnectMessageHandler(context, channel));
        endpoint.closeHandler(new CloseMessageHandler(context, channel));
        /**
         * 假如QoS级别是1(最多一次)，端点需要去处理来自客户端的PUBACK消息，为了收到最后的确认消息。可以使用publishAcknowledgeHandler方法。
         */
        endpoint.publishAcknowledgeMessageHandler(new PubAckMessageHandler(context, channel));
        /**
         * 假如QoS级别是2(正好一次)，端点需要去处理来自客户端的PUBREC消息。
         * 可以通过publishReceivedHandler方法来完成该操作。
         * 在该Handler内，端点可以使用publishRelease方法响应PUBREL消息给客户端。最后一步是处理来自客户端的PUBCOMP消息；
         * 可以使用publishCompleteHandler来指定一个handler当收到PUBCOMP消息时候调用。
         */
        endpoint.publishReleaseMessageHandler(new PublishPubRelMessageHandler(context, channel));
        endpoint.publishCompletionMessageHandler(new PublishPubCompMessageHandler(context, channel));
    }
}
