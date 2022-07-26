package com.iot.mqtt.event;

import lombok.Builder;
import lombok.Data;

/**
 * @author liangjiajun
 */
@Data
@Builder
public class SessionRefreshEvent {
    private String clientId;
}
