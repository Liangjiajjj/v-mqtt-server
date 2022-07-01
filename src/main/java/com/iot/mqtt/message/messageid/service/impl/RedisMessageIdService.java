package com.iot.mqtt.message.messageid.service.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.message.messageid.service.IMessageIdService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机的消息id生产器（集群继续用redis）
 *
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.broker.cluster_enabled", havingValue = "true")
public class RedisMessageIdService implements IMessageIdService {

    @Autowired
    private RedissonClient redissonClient;

    public int getNextMessageId() {
        try {
            while (true) {
                RAtomicLong messageIdNum = redissonClient.getAtomicLong(RedisKeyConstant.MESSAGE_ID_KEY.getKey());
                int nextMsgId = (int) (messageIdNum.incrementAndGet() % 65536);
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
