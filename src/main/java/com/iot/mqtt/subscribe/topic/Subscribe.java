package com.iot.mqtt.subscribe.topic;

import lombok.*;

import java.util.Objects;

/**
 * 订阅topic
 * @author liangjiajun
 */
@Data
@Builder
@ToString
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscribe that = (Subscribe) o;
        return Objects.equals(topicFilter, that.topicFilter) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicFilter, clientId);
    }

}
