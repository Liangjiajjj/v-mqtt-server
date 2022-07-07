package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.MqttWill;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.message.handler.base.IHandler;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 关闭服务handler
 *
 * @author liangjiajun
 */
@Service
public class CloseMessageHandler implements IHandler<Void> {

    @Autowired
    private IClientChannelManager clientChannelManager;
    @Autowired
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
                        false, false);
            }
        }
    }
}
