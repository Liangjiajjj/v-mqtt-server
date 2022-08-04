package com.iot.mqtt.info;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MqttAuth {

    private final String username;
    private final String password;

}
