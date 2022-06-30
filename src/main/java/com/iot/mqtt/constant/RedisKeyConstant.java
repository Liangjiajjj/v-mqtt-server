package com.iot.mqtt.constant;

import lombok.Getter;

@Getter
public enum RedisKeyConstant {

    CLIENT_SESSION(RedisKeyConstant.MQTT_SERVER_PRE + "session:%s")
    ,;

    private String key;

    RedisKeyConstant(String key) {
        this.key = key;
    }

    public final static String MQTT_SERVER_PRE = "mqtt-server:";

    public String getRedisKey(String... obj) {
        return String.format(key, obj);
    }

}
