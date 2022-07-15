package com.iot.mqtt.retain.manager;


import com.iot.mqtt.message.dup.PublishMessageStore;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.List;

/**
 * @author liangjiajun
 */
public interface IRetainMessageManager {
    /**
     * 存储retain标志消息
     */
    void put(String topicFilter, MqttPublishMessage retainMessageStore);

    /**
     * 获取retain消息
     */
    PublishMessageStore get(String topicFilter);

    /**
     * 删除retain标志消息
     */
    void remove(String topicFilter);

    /**
     * 判断指定topic的retain消息是否存在
     */
    boolean containsKey(String topicFilter);

    /**
     * 获取retain消息集合
     */
    List<PublishMessageStore> search(String topicFilter);

    /**
     * 如果客户端发送给服务端 publish 报文保留（RETAIN）为1，服务端必须保存这个消息和它对应的质量等级（QOS）
     * 以便它可以被分发给未来的主题名匹配的订阅者，一个新的订阅链接建立时，对应每个匹配主题名，如果存在最近消息，它就必须发送到客户端
     * <p>
     * 留标志为 1 且有效载荷为零字节的 PUBLISH 报文会被服务端当作正常消息处理，它会被发送给订阅主题匹配的客户端。
     * 此外，同一个主题下任何现存的保留消息必须被移除，因此这个主题之后的任何订阅者都不会收到一个保留消息
     * <p>
     * 如果客户端发给服务端的 PUBLISH 报文的保留标志位 0，服务端不能存储这个消息也不能移除或替换任何现存的保留消息
     * <p>
     * 对于发布者不定期发送状态消息这个场景，保留消息很有用。新的订阅者将会收到最近的状态。
     *
     * @param message
     * @param topicName
     **/
    default void handlerRetainMessage(MqttPublishMessage message, String topicName) {
        if (message.fixedHeader().isRetain()) {
            // 报错最后一条消息
            byte[] messageBytes = new byte[message.payload().readableBytes()];
            if (messageBytes.length == 0) {
                remove(topicName);
            } else {
                put(topicName, message);
            }
        }
    }
}
