package com.iot.mqtt.session;

import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttWill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Getter
@Builder
@AllArgsConstructor
public class ClientSession {

    private final String brokerId;

    private final String clientId;

    private final MqttWill will;

    private final Boolean isCleanSession;

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.put("brokerId", brokerId);
        object.put("clientId", clientId);
        object.put("isCleanSession", getIsCleanSession());
        if (Objects.nonNull(getWill()) && getWill().isWillFlag()) {
            object.put("will", getWill().toJson());
        }
        return object;
    }

    public ClientSession fromJson(JsonObject jsonObject) {
        String brokerId = jsonObject.getString("brokerId");
        String clientId = jsonObject.getString("clientId");
        Boolean isCleanSession = jsonObject.getBoolean("isCleanSession");
        MqttWill will = new MqttWill(jsonObject.getJsonObject("will"));
        return new ClientSession(brokerId, clientId, will, isCleanSession);
    }
}
