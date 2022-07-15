package com.iot.mqtt.context;

import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.IHandler;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author liangjiajun
 */
@Order(-1)
@Component
public class MqttServiceContext {

    @Autowired
    private ApplicationContext context;

    /**
     * 根据不同的MqttQoS，有不同的发送策略
     *
     * @param mqttQoS
     * @return
     */
    public IQosLevelMessageService getQosLevelMessageService(MqttQoS mqttQoS) {
        return context.getBean(mqttQoS.name() + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE, IQosLevelMessageService.class);
    }

    /**
     * 根据类型选择 Handler
     *
     * @param messageType
     * @return
     */
    public IHandler getMessageHandler(MqttMessageType messageType) {
        return context.getBean(messageType.name() + CommonConstant.MQTT_MESSAGE_HANDLER, IHandler.class);
    }
}
