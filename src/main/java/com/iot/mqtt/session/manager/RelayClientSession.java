package com.iot.mqtt.session.manager;

import com.iot.mqtt.dup.PublishMessageStore;
import lombok.Builder;
import lombok.Getter;
import java.util.LinkedList;

/**
 * 转发的会话
 *
 * @author liangjiajun
 */
@Getter
@Builder
public class RelayClientSession {

    /**
     * 服务器id
     */
    private String brokerId;
    /**
     * 客户端id
     */
    private String clientId;
    /**
     * mdk5 key
     */
    private Long md5Key;
    /**
     * 队列长度
     */
    private Integer count;
    /**
     * 转发队列，批量发送消息
     */
    private final LinkedList<PublishMessageStore> relayMessageQueue = new LinkedList<>();

    public void add(PublishMessageStore publishMessage) {
        relayMessageQueue.add(publishMessage);
        count++;
    }

    public void clear() {
        relayMessageQueue.clear();
        count = 0;
    }

    public boolean isCanPush(Integer max) {
        return count.equals(max);
    }

    public boolean isEmpty() {
        return relayMessageQueue.isEmpty();
    }
}
