package com.iot.mqtt.message.handler;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttWill;
import org.springframework.context.ApplicationContext;

/**
 * 关闭服务handler
 *
 * @author liangjiajun
 */
public class CloseMessageHandler extends BaseMessageHandler<Void> {

    public CloseMessageHandler(ApplicationContext context, ClientChannel channel) {
        super(context, channel);
    }

    @Override
    public void handle(Void v) {
        /**
         * 发送遗愿消息
         */
        MqttWill will = channel.getEndpoint().will();
        if (will.isWillFlag()) {
            channel.publish(will.getWillTopic(), will.getWillMessage(),
                    MqttQoS.valueOf(will.getWillQos()),
                    false, false);
        }
    }
}
