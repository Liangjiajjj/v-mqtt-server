package com.iot.mqtt.relay.message;

import com.iot.mqtt.type.RelayMessageType;

/**
 * @author liangjiajun
 */
public class RelayBagMessage extends RelayBaseMessage {

    public RelayBagMessage(Throwable throwable){
        super(throwable);
    }

    @Override
    public RelayMessageType getType() {
        return RelayMessageType.bag;
    }
}
