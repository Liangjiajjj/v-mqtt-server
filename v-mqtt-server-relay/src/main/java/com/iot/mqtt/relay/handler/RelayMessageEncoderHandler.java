package com.iot.mqtt.relay.handler;

import com.iot.mqtt.relay.message.*;
import com.iot.mqtt.type.RelayMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import static io.netty.buffer.ByteBufUtil.reserveAndWriteUtf8;
import static io.netty.buffer.ByteBufUtil.utf8Bytes;

/**
 * @author liangjiajun
 */
@ChannelHandler.Sharable
public class RelayMessageEncoderHandler extends MessageToMessageEncoder<RelayBaseMessage> {

    public static final RelayMessageEncoderHandler INSTANCE = new RelayMessageEncoderHandler();

    @Override
    protected void encode(ChannelHandlerContext ctx, RelayBaseMessage message, List<Object> out) throws Exception {
        out.add(doEncode(ctx, message));
    }

    static ByteBuf doEncode(ChannelHandlerContext ctx, RelayBaseMessage message) {
        RelayMessageType type = message.getType();
        switch (type) {
            case auth:
                return encodeAuthMessage(ctx, (RelayAuthMessage) message, type);
            case auth_ack:
                return encodeConnectMessage(ctx, type, ctx.alloc().buffer());
            case ping:
                return encodeConnectMessage(ctx, type, ctx.alloc().buffer());
            case pong:
                return encodeConnectMessage(ctx, type, ctx.alloc().buffer());
            case pub:
                return encodePubMessage(ctx, (RelayPublishMessage) message, type);
            default:
                throw new IllegalArgumentException(
                        "Unknown message type: " + message.getType().name());
        }
    }

    private static ByteBuf encodePubMessage(ChannelHandlerContext ctx, RelayPublishMessage publishMessage, RelayMessageType type) {
        int payloadBufferSize = 0;
        String clientId = publishMessage.getClientId();
        int clientIdBytes = nullableUtf8Bytes(clientId);
        payloadBufferSize += 2 + clientIdBytes;
        // byte[] relayMessage = publishMessage.getRelayMessage();
        ByteBuf relayMessage = publishMessage.getRelayMessage();
        int relayMessageSize = relayMessage.readableBytes();
        ByteBuf payloadBuf = ctx.alloc().buffer(relayMessageSize + payloadBufferSize);
        writeExactUTF8String(payloadBuf, clientId, clientIdBytes);
        payloadBuf.writeBytes(relayMessage);
        return encodeConnectMessage(ctx, type, payloadBuf);
    }

    private static ByteBuf encodeAuthMessage(ChannelHandlerContext ctx, RelayAuthMessage authMessage, RelayMessageType type) {
        int payloadBufferSize = 0;
        String userName = authMessage.getUserName();
        int userNameBytes = nullableUtf8Bytes(userName);
        payloadBufferSize += 2 + userNameBytes;
        String passWord = authMessage.getPassWord();
        int passwordBytes = nullableUtf8Bytes(passWord);
        payloadBufferSize += 2 + passwordBytes;
        ByteBuf authBuf = ctx.alloc().buffer(payloadBufferSize);
        writeExactUTF8String(authBuf, userName, userNameBytes);
        writeExactUTF8String(authBuf, passWord, passwordBytes);
        return encodeConnectMessage(ctx, type, authBuf);
    }

    private static ByteBuf encodeConnectMessage(ChannelHandlerContext ctx, RelayMessageType type, ByteBuf payload) {
        // size + type + payload.size
        int payloadSize = payload.readableBytes();
        ByteBuf buffer = ctx.alloc().buffer(4 + 2 + payloadSize);
        try {
            buffer.writeInt(payloadSize);
            buffer.writeByte(type.getType());
            buffer.writeBytes(payload);
        } finally {
            payload.release();
        }
        return buffer;
    }

    private static int nullableUtf8Bytes(String s) {
        return s == null ? 0 : utf8Bytes(s);
    }

    private static void writeExactUTF8String(ByteBuf buf, String s, int utf8Length) {
        buf.ensureWritable(utf8Length + 2);
        buf.writeShort(utf8Length);
        if (utf8Length > 0) {
            final int writtenUtf8Length = reserveAndWriteUtf8(buf, s, utf8Length);
            assert writtenUtf8Length == utf8Length;
        }
    }
}
