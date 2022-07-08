package com.iot.mqtt.subscribe.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liangjiajun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeOperation {

    private String brokerId;

    private Integer operation;

    private Subscribe subscribe;

}
