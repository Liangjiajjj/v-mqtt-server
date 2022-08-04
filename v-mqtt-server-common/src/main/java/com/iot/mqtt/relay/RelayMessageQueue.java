package com.iot.mqtt.relay;

import com.iot.mqtt.dup.PublishMessageStore;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;

/**
 * 转发的会话队列
 *
 * @author liangjiajun
 */
@Getter
public class RelayMessageQueue {

    /**
     * 队列长度
     */
    private Integer count = 0;
    /**
     * 转发队列，批量发送消息
     */
    private final LinkedList<PublishMessageStore> relayMessageQueue = new LinkedList<>();

    public void add(PublishMessageStore publishMessage) {
        relayMessageQueue.add(publishMessage);
        count++;
    }

    public boolean isCanPush(Integer max) {
        return count.equals(max);
    }

    public boolean isEmpty() {
        return relayMessageQueue.isEmpty();
    }

    public void clear() {
        relayMessageQueue.clear();
        count = 0;
    }

}
