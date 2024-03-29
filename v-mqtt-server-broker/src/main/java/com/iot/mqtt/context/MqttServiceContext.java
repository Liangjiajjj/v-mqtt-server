package com.iot.mqtt.context;

import com.iot.mqtt.config.MqttConfig;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.handler.IMessageHandler;
import com.iot.mqtt.message.api.IQosLevelMessageService;
import com.iot.mqtt.thread.MqttEventExecuteGroup;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * @author liangjiajun
 */
@Order(-1)
@Component
public class MqttServiceContext {

    @Resource
    private MqttConfig mqttConfig;
    @Resource
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
    public IMessageHandler getMessageHandler(MqttMessageType messageType) {
        return context.getBean(messageType.name() + CommonConstant.MQTT_MESSAGE_HANDLER, IMessageHandler.class);
    }

    @Bean(value = "PUBLISH-EXECUTOR")
    public MqttEventExecuteGroup publishExecutor() {
        Integer nThreads = Optional.ofNullable(mqttConfig.getPushThreads())
                .orElse(Runtime.getRuntime().availableProcessors());
        return new MqttEventExecuteGroup(nThreads, new DefaultThreadFactory("PUBLISH-EXECUTOR"));
    }

    @Bean(value = "RELAY-PUBLISH-SERVER-EXECUTOR")
    public MqttEventExecuteGroup relayPublishServerExecutor() {
        Integer nThreads = Optional.ofNullable(mqttConfig.getRelayPushServerThreads())
                .orElse(Runtime.getRuntime().availableProcessors());
        return new MqttEventExecuteGroup(nThreads, new DefaultThreadFactory("RELAY-PUBLISH-SERVER-EXECUTOR"));
    }

    @Bean(value = "RELAY-PUBLISH-CLIENT-EXECUTOR")
    public MqttEventExecuteGroup relayPublishClientExecutor() {
        Integer nThreads = Optional.ofNullable(mqttConfig.getRelayPushClientThreads())
                .orElse(Runtime.getRuntime().availableProcessors());
        return new MqttEventExecuteGroup(nThreads, new DefaultThreadFactory("RELAY-PUBLISH-CLIENT-EXECUTOR"));
    }

}
