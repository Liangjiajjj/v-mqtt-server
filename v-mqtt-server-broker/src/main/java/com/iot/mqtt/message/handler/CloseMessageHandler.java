package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.info.MqttWill;
import com.iot.mqtt.channel.manager.api.IClientChannelManager;
import com.iot.mqtt.handler.IMessageHandler;
import com.iot.mqtt.messageid.api.IMessageIdService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.api.IClientSessionManager;
import com.iot.mqtt.subscribe.api.ISubscribeManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 关闭服务handler
 *
 * @author liangjiajun
 */
@Service
public class CloseMessageHandler implements IMessageHandler<Void> {

    @Resource
    private ISubscribeManager subscribeManager;
    @Resource
    private IMessageIdService messageIdService;
    @Resource
    private IClientChannelManager clientChannelManager;
    @Resource
    private IClientSessionManager clientSessionManager;

    @Override
    public void handle(Channel channel, Void unused) {
        /**
         * 发送遗愿消息
         */
        String clientId = getClientId(channel);
        ClientChannel clientChannel = clientChannelManager.get(clientId);
        ClientSession clientSession = clientSessionManager.get(clientId);
        if (Objects.nonNull(clientSession)) {
            MqttWill will = clientSession.getWill();
            if (will.isWillFlag()) {
                clientChannel.publish(will.getWillTopic(), will.getWillMessage(),
                        MqttQoS.valueOf(will.getWillQos()),
                        false, false, messageIdService.getNextMessageId());
            }
            if (clientSession.getIsCleanSession()) {
                subscribeManager.removeForClient(clientId);
            }
        }
    }
}
