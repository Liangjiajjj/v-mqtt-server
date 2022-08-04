package com.iot.mqtt.messageid.impl;

import com.iot.mqtt.messageid.api.IMessageIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机的消息id生产器（集群继续用redis）
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "false")
public class CacheMessageIdService implements IMessageIdService {

    private final AtomicInteger messageId = new AtomicInteger();

    @Override
    public int getNextMessageId() {
        try {
            while (true) {
                int nextMsgId = messageId.incrementAndGet() % 65536;
                if (nextMsgId > 0) {
                    return nextMsgId;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

}
