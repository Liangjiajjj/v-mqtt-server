package com.iot.mqtt.relay.message;

import com.iot.mqtt.type.RelayMessageType;

/**
 * @author liangjiajun
 */
public class RelayPongMessage extends RelayBaseMessage {

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.pong;
    }
}
