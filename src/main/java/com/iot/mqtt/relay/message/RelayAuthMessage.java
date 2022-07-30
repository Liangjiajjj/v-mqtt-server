package com.iot.mqtt.relay.message;

import com.iot.mqtt.relay.message.type.RelayMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author liangjiajun
 */
@Data
@AllArgsConstructor
public class RelayAuthMessage extends RelayBaseMessage {

    private String userName;
    private String passWord;

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.auth;
    }
}
