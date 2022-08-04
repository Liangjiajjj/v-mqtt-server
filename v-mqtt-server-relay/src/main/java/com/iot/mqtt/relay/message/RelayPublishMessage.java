package com.iot.mqtt.relay.message;

import com.iot.mqtt.type.RelayMessageType;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author liangjiajun
 */
@Data
@AllArgsConstructor
public class RelayPublishMessage extends RelayBaseMessage {

    /**
     * clientId
     */
    private final String clientId;
    /**
     * 转发的协议
     */
    private final ByteBuf relayMessage;

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.pub;
    }
}
