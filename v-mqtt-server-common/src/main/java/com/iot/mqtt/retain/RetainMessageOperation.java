package com.iot.mqtt.retain;

import com.iot.mqtt.dup.PublishMessageStore;
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
public class RetainMessageOperation {

    private String brokerId;

    private Integer operation;

    private PublishMessageStore publishMessageStore;

}
