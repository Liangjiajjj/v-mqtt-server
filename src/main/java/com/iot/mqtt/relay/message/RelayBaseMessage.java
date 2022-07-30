package com.iot.mqtt.relay.message;

import com.iot.mqtt.relay.message.type.RelayMessageType;
import lombok.Data;

/**
 * @author liangjiajun
 */
@Data
public abstract class RelayBaseMessage {

    public RelayBaseMessage() {

    }

    public RelayBaseMessage(Throwable cause) {
        this.cause = cause;
    }

    private Throwable cause;

    /**
     * 消息类型
     *
     * @return
     */
    public abstract RelayMessageType getType();
}
