package com.iot.mqtt.subscribe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 订阅topic
 * @author liangjiajun
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
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
    private Integer mqttQoS;

}
