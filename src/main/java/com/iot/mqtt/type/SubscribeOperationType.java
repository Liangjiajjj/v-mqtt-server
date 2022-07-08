package com.iot.mqtt.type;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author liangjiajun
 */
public enum SubscribeOperationType {
    /**
     * 添加
     */
    ADD(1),
    /**
     * 删除
     */
    REMOVE(2);

    @Getter
    private final Integer type;

    SubscribeOperationType(Integer type) {
        this.type = type;
    }

    public static SubscribeOperationType getSubscribeOperationType(Integer type) {
        return Arrays.stream(values()).filter((sub) -> sub.type.equals(type)).findFirst().orElse(null);
    }
}
