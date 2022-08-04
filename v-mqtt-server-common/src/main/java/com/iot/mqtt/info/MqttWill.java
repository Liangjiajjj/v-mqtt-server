package com.iot.mqtt.info;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Base64;

@Data
@AllArgsConstructor
public class MqttWill {
    private final boolean isWillFlag;
    private final String willTopic;
    private final byte[] willMessage;
    private final int willQos;
    private final boolean isWillRetain;
    private final MqttProperties willProperties;

    public MqttWill(JSONObject json) {
        this.isWillFlag = json.getBoolean("isWillFlag");
        this.willTopic = json.getString("willTopic");
        this.willMessage = json.getBytes("willMessage");
        this.willQos = json.getInteger("willQos");
        this.isWillRetain = json.getBoolean("isWillRetain");
        this.willProperties = propertiesFromJson(json.getJSONArray("willProperties"));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("isWillFlag", this.isWillFlag);
        json.put("willTopic", this.willTopic);
        json.put("willMessage", this.willMessage);
        json.put("willQos", this.willQos);
        json.put("isWillRetain", this.isWillRetain);
        json.put("willProperties", propertiesToJson(this.willProperties));
        return json;
    }
    public static JSONArray propertiesToJson(MqttProperties properties) {
        JSONArray array = new JSONArray();
        for(MqttProperties.MqttProperty<?> prop: properties.listAll()) {
            array.add(propertyToJson(prop));
        }
        return array;
    }

    public static JSONObject propertyToJson(MqttProperties.MqttProperty<?> prop) {
        JSONObject obj = new JSONObject();
        if (prop instanceof MqttProperties.StringProperty ||
                prop instanceof MqttProperties.IntegerProperty) {
            obj.put("id", prop.propertyId());
            obj.put("val", prop.value());
        } else if (prop instanceof MqttProperties.BinaryProperty) {
            obj.put("id", prop.propertyId());
            String value = Base64.getEncoder().encodeToString(((MqttProperties.BinaryProperty) prop).value());
            obj.put("val", value);
        } else if (prop instanceof MqttProperties.UserProperties) {
            for (MqttProperties.StringPair kv : ((MqttProperties.UserProperties) prop).value()) {
                obj.put("id", prop.propertyId());
                obj.put("key", kv.key);
                obj.put("val", kv.value);
            }
        }
        return obj;
    }

    public static MqttProperties propertiesFromJson(JSONArray array) {
        MqttProperties props = new MqttProperties();
        for (Object item : array) {
            props.add(propertyFromJson((JSONObject) item));
        }
        return props;
    }

    public static MqttProperties.MqttProperty<?> propertyFromJson(JSONObject obj) {
        int id = obj.getInteger("id");

        MqttProperties.MqttPropertyType propType = MqttProperties.MqttPropertyType.valueOf(id);
        switch (propType) {
            case PAYLOAD_FORMAT_INDICATOR:
            case REQUEST_PROBLEM_INFORMATION:
            case REQUEST_RESPONSE_INFORMATION:
            case MAXIMUM_QOS:
            case RETAIN_AVAILABLE:
            case WILDCARD_SUBSCRIPTION_AVAILABLE:
            case SUBSCRIPTION_IDENTIFIER_AVAILABLE:
            case SHARED_SUBSCRIPTION_AVAILABLE:
            case SERVER_KEEP_ALIVE:
            case RECEIVE_MAXIMUM:
            case TOPIC_ALIAS_MAXIMUM:
            case TOPIC_ALIAS:
            case PUBLICATION_EXPIRY_INTERVAL:
            case SESSION_EXPIRY_INTERVAL:
            case WILL_DELAY_INTERVAL:
            case MAXIMUM_PACKET_SIZE:
            case SUBSCRIPTION_IDENTIFIER:
                return new MqttProperties.IntegerProperty(id, obj.getInteger("val"));
            case CONTENT_TYPE:
            case RESPONSE_TOPIC:
            case ASSIGNED_CLIENT_IDENTIFIER:
            case AUTHENTICATION_METHOD:
            case RESPONSE_INFORMATION:
            case SERVER_REFERENCE:
            case REASON_STRING:
                return new MqttProperties.StringProperty(id, obj.getString("val"));
            case CORRELATION_DATA:
            case AUTHENTICATION_DATA:
                return new MqttProperties.BinaryProperty(id, Base64.getDecoder().decode(obj.getString("val")));
            case USER_PROPERTY:
                String key = obj.getString("key");
                return new MqttProperties.UserProperty(key, obj.getString("val"));
            default:
                throw new IllegalArgumentException("Unsupported property type: " + propType);
        }
    }
}
