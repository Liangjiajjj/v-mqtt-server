package com.iot.mqtt.relay.message.type;

import lombok.Getter;

/**
 * @author liangjiajun
 */

public enum RelayMessageType {
    bag((byte) 0),

    auth((byte) 1),

    auth_ack((byte) 2),

    ping((byte) 3),

    pong((byte) 4),

    pub((byte) 5),;

    @Getter
    private byte type;

    RelayMessageType(byte type) {
        this.type = type;
    }

    public static RelayMessageType getRelayMessageType(byte type) {
        for (RelayMessageType messageType : values()) {
            if (messageType.type == type) {
                return messageType;
            }
        }
        return null;
    }
    }
