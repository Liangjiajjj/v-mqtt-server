package com.iot.mqtt.session;

import lombok.*;

/**
 * @author liangjiajun
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalClientSession {

    private ClientSession clientSession;

    private Integer setListenerId;

    private Integer deletedListenerId;

    private Integer expiredListenerId;
}
