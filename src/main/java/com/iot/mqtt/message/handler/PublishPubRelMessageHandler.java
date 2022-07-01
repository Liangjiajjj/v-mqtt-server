package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.message.dup.DupPubRelMessage;
import com.iot.mqtt.message.dup.manager.IDupPubRelMessageManager;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.session.ClientSession;
import io.netty.util.AttributeKey;
import io.vertx.mqtt.messages.MqttPubRelMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 设备信息应答包
 *
 * @author liangjiajun
 */
@Slf4j
public class PublishPubRelMessageHandler extends BaseMessageHandler<MqttPubRelMessage> {

    private final IDupPublishMessageManager dupPublishMessageManager;

    private final IDupPubRelMessageManager dupPubRelMessageManager;

    public PublishPubRelMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
        this.dupPublishMessageManager = context.getBean(IDupPublishMessageManager.class);
        this.dupPubRelMessageManager = context.getBean(IDupPubRelMessageManager.class);
    }

    @Override
    public void handle(MqttPubRelMessage mqttPubRelMessage) {
        log.debug("PUBREL - clientId: {}, messageId: {}", channel.getClientId(), mqttPubRelMessage.messageId());
        // 收到应答包，删除重试消息
        dupPublishMessageManager.remove(channel.getClientId(), mqttPubRelMessage.messageId());
        // 加入重新确认列表
        dupPubRelMessageManager.put(channel.getClientId(), DupPubRelMessage.builder()
                .messageId(mqttPubRelMessage.messageId())
                .clientId(channel.getClientId()).build());
        // PUCREC
        channel.publishReceived(mqttPubRelMessage.messageId());
    }

}

