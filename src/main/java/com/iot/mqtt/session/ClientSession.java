package com.iot.mqtt.session;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.channel.MqttWill;
import lombok.*;

import java.util.Objects;
import java.util.Optional;

/**
 * @author liangjiajun
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSession {

    private String brokerId;

    private String clientId;

    private Long md5Key;

    private int expire;

    private MqttWill will;

    private Boolean isCleanSession;

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("brokerId", brokerId);
        object.put("clientId", clientId);
        object.put("md5_key", md5Key);

        object.put("expire", expire);
        object.put("isCleanSession", isCleanSession);
        if (Objects.nonNull(getWill()) && getWill().isWillFlag()) {
            object.put("will", getWill().toJson());
        }
    /*    object.put("setListenerId", setListenerId);
        object.put("deletedListenerId", deletedListenerId);
        object.put("expiredListenerId", expiredListenerId);*/
        return object;
    }

    public ClientSession fromJson(JSONObject jsonObject) {
        String brokerId = jsonObject.getString("brokerId");
        String clientId = jsonObject.getString("clientId");
        Integer expire = jsonObject.getInteger("expire");
        Long md5Key = jsonObject.getLong("md5_key");
        Boolean isCleanSession = jsonObject.getBoolean("isCleanSession");
        MqttWill will = Optional.ofNullable(jsonObject.getJSONObject("will")).map(MqttWill::new).orElse(null);
     /*   Integer setListenerId = jsonObject.getInteger("setListenerId");
        Integer deletedListenerId = jsonObject.getInteger("deletedListenerId");
        Integer expiredListenerId = jsonObject.getInteger("expiredListenerId");*/
        return new ClientSession(brokerId, clientId, md5Key, expire, will, isCleanSession);
        // setListenerId, deletedListenerId, expiredListenerId);
    }
}
