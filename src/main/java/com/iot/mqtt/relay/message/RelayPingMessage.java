package com.iot.mqtt.relay.message;

import com.iot.mqtt.relay.message.type.RelayMessageType;

/**
 * @author liangjiajun
 */
public class RelayPingMessage extends RelayBaseMessage {

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.ping;
    }
}
