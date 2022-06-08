package com.iot.mqtt.message.handler;

import com.iot.mqtt.session.ClientSession;
import io.vertx.core.Handler;
import io.vertx.mqtt.messages.MqttMessage;
import io.vertx.mqtt.messages.MqttPubCompMessage;
import io.vertx.mqtt.messages.MqttPubRelMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@Slf4j
@AllArgsConstructor
public abstract class BaseMessageHandler<E> implements Handler<E> {

    protected final ApplicationContext context;
    protected final ClientSession clientSession;

    @Override
    public void handle(E e) {
        try {
            clientSession.getExecutor().execute(() -> handle(e));
        } catch (Throwable throwable) {
            if (e instanceof MqttMessage) {
                log.error("MessageHandler handle error messageId {} ", ((MqttMessage) e).messageId(), throwable);
            } else if (e instanceof MqttPubCompMessage) {
                log.error("MessageHandler handle error messageId {} ", ((MqttPubCompMessage) e).messageId(), throwable);
            } else if (e instanceof MqttPubRelMessage) {
                log.error("MessageHandler handle error messageId {} ", ((MqttPubRelMessage) e).messageId(), throwable);
            } else {
                log.error("MessageHandler handle error", throwable);
            }
        }
    }

}
