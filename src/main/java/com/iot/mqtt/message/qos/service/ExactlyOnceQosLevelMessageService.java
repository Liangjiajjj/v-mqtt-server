package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 暂时不支持qos
 * QoS2可在业务层做，比如在payload中增加去重标记,减少发包数量
 * <p>
 * qos2：确保一次送达，我给你发 (PUBLISH)，你给我回一个你收到了 (PUBREC)，我再给你发
 * 一 个你确定你收到了吗 （PUBREL），你再给我回一个收到了别发了求你了 (PUBCOMP)
 *
 * @author liangjiajun
 */

@Service(value = CommonConstant.EXACTLY_ONCE + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE)
public class ExactlyOnceQosLevelMessageService extends BaseQosLevelMessageService {

    @Autowired
    private IDupPublishMessageManager dupPublishMessageManager;

    @Override
    public void publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message) {
        String toClientId = subscribe.getClientId();
        publish0(toClientId, message);
        // qos = 1/2 需要保存消息，确保发送到位
        dupPublishMessageManager.put(toClientId, message);
    }

    @Override
    public void publishReply(ClientChannel channel, Integer messageId) {
        channel.publishRelease(messageId);
    }
}
