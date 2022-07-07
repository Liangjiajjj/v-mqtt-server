package com.iot.mqtt.message.handler.base;

import com.iot.mqtt.channel.ClientChannel;
import com.iot.mqtt.channel.manager.IClientChannelManager;
import com.iot.mqtt.constant.CommonConstant;
import com.iot.mqtt.message.handler.base.IHandler;
import com.iot.mqtt.message.qos.service.IQosLevelMessageService;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMessageHandler<E extends MqttMessage> implements IHandler<E> {


    @Autowired
    protected ApplicationContext context;
    @Autowired
    private IClientChannelManager clientChannelManager;

    @Override
    public void handle(Channel channel, E e) {
        String clientId = getClientId(channel);
        if (log.isTraceEnabled()) {
            log.trace("MessageHandler handle clientId {} , threadName {} ", clientId, Thread.currentThread().getName());
        }
        ClientChannel clientChannel = clientChannelManager.get(clientId);
        if (Objects.isNull(clientChannel)) {
            log.warn("MessageHandler handle clientChannel is null {} ", clientId);
            return;
        }
        clientChannel.getExecutor().execute(() -> {
            try {
                handle0(clientChannel, e);
            } catch (Throwable throwable) {
                log.error("MessageHandler handle0 error channelId {} , messageId {} ", channel.id().asLongText(), e.fixedHeader().messageType(), throwable);
            } finally {
                ReferenceCountUtil.release(e);
            }
        });
    }

    /**
     * 处理业务逻辑
     *
     * @param e
     */
    public void handle0(ClientChannel clientChannel, E e) {
    }

    /**
     * 根据不同的MqttQoS，有不同的发送策略
     *
     * @param mqttQoS
     * @return
     */
    protected IQosLevelMessageService getQosLevelMessageService(MqttQoS mqttQoS) {
        return context.getBean(mqttQoS.name() + CommonConstant.QOS_LEVEL_MESSAGE_SERVICE, IQosLevelMessageService.class);
    }

}
