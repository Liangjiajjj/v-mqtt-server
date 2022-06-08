package com.iot.mqtt.subscribe.manager;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订阅topic
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Subscribe {

    /**
     * 客户端id
     */
    private String clientId;
    /**
     * 主题过滤器
     * 订阅中包含一个表达式，用于表示一个或者多个主题。可以使用通配符
     */
    private String topicFilter;
    /**
     * 服务质量
     */
    private MqttQoS mqttQoS;

}
