package com.iot.mqtt.message.qos.service;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.dup.manager.IDupPublishMessageManager;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import com.iot.mqtt.session.ClientSession;
import com.iot.mqtt.session.manager.IClientSessionManager;
import com.iot.mqtt.subscribe.Subscribe;
import io.vertx.core.Future;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
    public Future<Integer> publish(ClientChannel channel, Subscribe subscribe, MqttPublishMessage message) {
        String toClientId = subscribe.getClientId();
        Future<Integer> future = publish0(toClientId, message);
        // qos = 1/2 需要保存消息，确保发送到位
        dupPublishMessageManager.put(toClientId, message);
        return future;
    }

    @Override
    public void publishReply(ClientChannel channel, MqttPublishMessage message) {
        channel.publishRelease(message.messageId());
    }
}
