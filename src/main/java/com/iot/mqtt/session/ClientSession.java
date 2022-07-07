package com.iot.mqtt.session;

import com.alibaba.fastjson.JSONObject;
import com.iot.mqtt.channel.MqttWill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Optional;

/**
 * @author liangjiajun
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSession {

    private String brokerId;

    private String clientId;

    private int expire;

    private MqttWill will;

    private Boolean isCleanSession;

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("brokerId", brokerId);
        object.put("clientId", clientId);
        object.put("expire", expire);
        object.put("isCleanSession", isCleanSession);
        if (Objects.nonNull(getWill()) && getWill().isWillFlag()) {
            object.put("will", getWill().toJson());
        }
        return object;
    }

    public ClientSession fromJson(JSONObject jsonObject) {
        String brokerId = jsonObject.getString("brokerId");
        String clientId = jsonObject.getString("clientId");
        Integer expire = jsonObject.getInteger("expire");
        Boolean isCleanSession = jsonObject.getBoolean("isCleanSession");
        MqttWill will = Optional.ofNullable(jsonObject.getJSONObject("will")).map(MqttWill::new).orElse(null);
        return new ClientSession(brokerId, clientId, expire, will, isCleanSession);
    }
}
