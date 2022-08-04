package com.iot.mqtt.relay.handler;

import com.iot.mqtt.relay.message.*;
import com.iot.mqtt.type.RelayMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author liangjiajun
 */
@Slf4j
public class RelayMessageDecoderHandler extends ReplayingDecoder<RelayMessageDecoderHandler.State> {

    private int length;

    enum State {
        HEAD,
        PAYLOAD,
        BAD_MESSAGE,
    }

    public RelayMessageDecoderHandler() {
        super(RelayMessageDecoderHandler.State.HEAD);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        State state = state();
        switch (state) {
            case HEAD:
                try {
                    length = buffer.readInt();
                    checkpoint(RelayMessageDecoderHandler.State.PAYLOAD);
                    // fall through
                } catch (Exception cause) {
                    log.error("RelayMessageDecoderHandler decode HEAD error !!! ", cause);
                    out.add(invalidMessage(cause));
                    return;
                }
                break;
            case PAYLOAD:
                byte type = buffer.readByte();
                ByteBuf payload = buffer.readBytes(length);
                try {
                    RelayMessageType relayMessageType = RelayMessageType.getRelayMessageType(type);
                    switch (Objects.requireNonNull(relayMessageType)) {
                        case auth:
                            Result<String> userName = decodeString(payload);
                            Result<String> passWord = decodeString(payload);
                            out.add(new RelayAuthMessage(userName.value, passWord.value));
                            break;
                        case auth_ack:
                            out.add(new RelayAuthAckMessage());
                            break;
                        case ping:
                            out.add(new RelayPingMessage());
                            break;
                        case pong:
                            out.add(new RelayPongMessage());
                            break;
                        case pub:
                            try {
                                Result<String> clientId = decodeString(payload);
                                // payloadLength = allLength - clientIdLength
                                ByteBuf payloadBytes = payload.readRetainedSlice(length - clientId.numberOfBytesConsumed);
                                out.add(new RelayPublishMessage(clientId.value, payloadBytes));
                            } catch (Exception cause) {
                                log.error("RelayMessageDecoderHandler decode pub payload error !!! type {}", type, cause);
                            }
                            break;
                        default:
                            throw new RuntimeException("not message type !!!");
                    }
                    checkpoint(RelayMessageDecoderHandler.State.HEAD);
                    break;
                    // fall through
                } catch (Throwable cause) {
                    out.add(invalidMessage(cause));
                    log.error("RelayMessageDecoderHandler decode PAYLOAD error !!! type {}", type, cause);
                    return;
                } finally {
                    ReferenceCountUtil.release(payload);
                }
            case BAD_MESSAGE:
                // Keep discarding until disconnection.
                buffer.skipBytes(actualReadableBytes());
                break;
            default:
                // Shouldn't reach here.
                log.error("RelayMessageDecoderHandler error !!! not type {}", state);
                throw new Error();
        }

    }

    private RelayBagMessage invalidMessage(Throwable cause) {
        checkpoint(State.BAD_MESSAGE);
        return new RelayBagMessage(cause);
    }

    private static Result<String> decodeString(ByteBuf buffer) {
        return decodeString(buffer, 0, Integer.MAX_VALUE);
    }

    private static Result<String> decodeString(ByteBuf buffer, int minBytes, int maxBytes) {
        int size = decodeMsbLsb(buffer);
        int numberOfBytesConsumed = 2;
        if (size < minBytes || size > maxBytes) {
            buffer.skipBytes(size);
            numberOfBytesConsumed += size;
            return new Result<>(null, numberOfBytesConsumed);
        }
        String s = buffer.toString(buffer.readerIndex(), size, CharsetUtil.UTF_8);
        buffer.skipBytes(size);
        numberOfBytesConsumed += size;
        return new Result<>(s, numberOfBytesConsumed);
    }

    private static byte[] decodeByteArray(ByteBuf buffer) {
        int size = decodeMsbLsb(buffer);
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static int decodeMsbLsb(ByteBuf buffer) {
        int min = 0;
        int max = 65535;
        short msbSize = buffer.readUnsignedByte();
        short lsbSize = buffer.readUnsignedByte();
        int result = msbSize << 8 | lsbSize;
        if (result < min || result > max) {
            result = -1;
        }
        return result;
    }

    private static final class Result<T> {

        private final T value;
        private final int numberOfBytesConsumed;

        Result(T value, int numberOfBytesConsumed) {
            this.value = value;
            this.numberOfBytesConsumed = numberOfBytesConsumed;
        }
    }


}
