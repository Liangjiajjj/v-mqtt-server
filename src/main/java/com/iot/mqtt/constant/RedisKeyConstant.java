package com.iot.mqtt.constant;

import lombok.Getter;

@Getter
public enum RedisKeyConstant {

    /**
     * 会话缓存
     */
    CLIENT_SESSION_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "session:%s"),
    /**
     * 订阅列表
     */
    SUBSCRIBE_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "subscribe:%s"),
    /**
     * 订阅列表set
     */
    SUBSCRIBE_SET_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "subscribe_set:%s"),
    /**
     * 遗愿消息
     */
    RETAIN_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "retain_message"),
    /**
     * 消息id
     */
    MESSAGE_ID_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "messageId"),
    /**
     * qos1
     */
    DUP_PUBLISH_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "duppublish:%s"),
    /**
     * qos2
     */
    DUP_PUBREL_KEY(RedisKeyConstant.MQTT_SERVER_PRE + "duppubrel:%s"),
    /**
     * 转发队列
     */
    RELAY_MESSAGE_TOPIC(RedisKeyConstant.MQTT_SERVER_PRE + "relay_message_topic:%s"),
    /**
     * 同步订阅主题
     */
    SYN_SUBSCRIBE_TOPIC(RedisKeyConstant.MQTT_SERVER_PRE + "syn_subscribe_topic"),
    /**
     * 同步订阅保留消息
     */
    SYN_RETAIN_MESSAGE_TOPIC(RedisKeyConstant.MQTT_SERVER_PRE + "syn_retain_message_topic")
    ;

    private final String key;

    RedisKeyConstant(String key) {
        this.key = key;
    }

    public final static String MQTT_SERVER_PRE = "mqtt-server:";

    public String getKey(String... obj) {
        return String.format(key, obj);
    }

}
