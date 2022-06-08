package com.iot.mqtt.message.handler;

import com.iot.mqtt.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttWill;
import org.springframework.context.ApplicationContext;

/**
 * 关闭服务handler
 */
public class CloseMessageHandler extends BaseMessageHandler<Void> {

    public CloseMessageHandler(ApplicationContext context, ClientSession clientSession) {
        super(context, clientSession);
    }

    @Override
    public void handle(Void v) {
        /**
         * 发送遗愿消息
         */
        MqttWill will = clientSession.getMqttWill();
        if (will.isWillFlag()) {
            clientSession.getEndpoint().publish(will.getWillTopic(), will.getWillMessage(),
                    MqttQoS.valueOf(will.getWillQos()),
                    false, false);
        }
    }
}
