package com.iot.mqtt.messageid.impl;

import com.iot.mqtt.constant.RedisKeyConstant;
import com.iot.mqtt.messageid.api.IMessageIdService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 * @author liangjiajun
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisMessageIdServiceImpl implements IMessageIdService {

    @Resource
    private RedissonClient redissonClient;

    @Override
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
