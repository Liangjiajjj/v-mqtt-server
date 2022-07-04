package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import com.iot.mqtt.message.retain.manager.IRetainMessageManager;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 设备信息包
 *
 * @author liangjiajun
 */
@Slf4j
public class PublishMessageHandler extends BaseMessageHandler<MqttPublishMessage> {

    private final ISubscribeManager subscribeManager;
    private final IRetainMessageManager retainMessageManager;

    public PublishMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.subscribeManager = context.getBean(ISubscribeManager.class);
        this.retainMessageManager = context.getBean(IRetainMessageManager.class);
    }

    @Override
    public void handle(MqttPublishMessage message) {
        String topicName = message.topicName();
        if (log.isTraceEnabled()) {
            log.trace("received clientId : {} topic : {} qoS : {} message : {}", channel.getClientId(), topicName, message.qosLevel(), new String(message.payload().getBytes(), StandardCharsets.UTF_8));
        }
        // 发送到订阅消息的客户端
        Collection<Subscribe> subscribes = subscribeManager.search(topicName);
        subscribes.forEach(subscribe -> {
            // 发送消息到订阅的topic
            IQosLevelMessageService qosLevelMessageService = getQosLevelMessageService(message.qosLevel());
            qosLevelMessageService.publish(channel, subscribe, message);
            handlerRetainMessage(message, topicName);
        });
        // 返回客户端
        IQosLevelMessageService qosLevelMessageService = getQosLevelMessageService(message.qosLevel());
        qosLevelMessageService.publishReply(channel, message);
    }


    /**
     * 如果客户端发送给服务端 publish 报文保留（RETAIN）为1，服务端必须保存这个消息和它对应的质量等级（QOS）
     * 以便它可以被分发给未来的主题名匹配的订阅者，一个新的订阅链接建立时，对应每个匹配主题名，如果存在最近消息，它就必须发送到客户端
     * <p>
     * 留标志为 1 且有效载荷为零字节的 PUBLISH 报文会被服务端当作正常消息处理，它会被发送给订阅主题匹配的客户端。
     * 此外，同一个主题下任何现存的保留消息必须被移除，因此这个主题之后的任何订阅者都不会收到一个保留消息
     * <p>
     * 如果客户端发给服务端的 PUBLISH 报文的保留标志位 0，服务端不能存储这个消息也不能移除或替换任何现存的保留消息
     * <p>
     * 对于发布者不定期发送状态消息这个场景，保留消息很有用。新的订阅者将会收到最近的状态。
     **/
    private void handlerRetainMessage(MqttPublishMessage message, String topicName) {
        if (message.isRetain()) {
            if (message.payload().getBytes().length == 0) {
                retainMessageManager.remove(topicName);
            } else {
                retainMessageManager.put(topicName, message);
            }
        }
    }

}