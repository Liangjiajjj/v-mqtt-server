package com.iot.mqtt.type;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author liangjiajun
 */
public enum OperationType {
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

    OperationType(Integer type) {
        this.type = type;
    }

    public static OperationType getOperationType(Integer type) {
        return Arrays.stream(values()).filter((sub) -> sub.type.equals(type)).findFirst().orElse(null);
    }
}
