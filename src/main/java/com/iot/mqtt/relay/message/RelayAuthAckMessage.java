package com.iot.mqtt.relay.message;

import com.iot.mqtt.relay.message.type.RelayMessageType;
import lombok.Data;

/**
 * @author liangjiajun
 */
@Data
public class RelayAuthAckMessage extends RelayBaseMessage {

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.auth_ack;
    }
}
