package com.iot.mqtt.util.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author liangjiajun
 */
public class ChannelFutures {

    private ChannelFutures() {
        throw new AssertionError("Class with static utility methods only cannot be instantiated");
    }

    /**
     * Convert a {@link ChannelFuture} into a {@link CompletableFuture}.
     *
     * @param channelFuture the {@link ChannelFuture}
     * @return a {@link CompletableFuture} that completes successfully when the channelFuture completes successfully,
     *         and completes exceptionally if the channelFuture completes with a {@link Throwable}
     */
    public static CompletableFuture<Channel> toCompletableFuture(ChannelFuture channelFuture) {
        Objects.requireNonNull(channelFuture, "channelFuture cannot be null");

        CompletableFuture<Channel> adapter = new CompletableFuture<>();
        if (channelFuture.isDone()) {
            if (channelFuture.isSuccess()) {
                adapter.complete(channelFuture.channel());
            } else {
                adapter.completeExceptionally(channelFuture.cause());
            }
        } else {
            channelFuture.addListener((ChannelFuture cf) -> {
                if (cf.isSuccess()) {
                    adapter.complete(cf.channel());
                } else {
                    adapter.completeExceptionally(cf.cause());
                }
            });
        }
        return adapter;
    }

}
