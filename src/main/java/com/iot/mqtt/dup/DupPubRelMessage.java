package com.iot.mqtt.dup;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author liangjiajun
 */
@Data
@Builder
public class DupPubRelMessage implements Serializable {

    private static final long serialVersionUID = -4111642532532950980L;

    private String clientId;

    private int messageId;

}
