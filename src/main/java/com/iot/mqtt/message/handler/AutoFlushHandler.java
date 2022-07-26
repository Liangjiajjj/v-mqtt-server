package com.iot.mqtt.message.handler;


import com.iot.mqtt.message.handler.base.IHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Auto-flush data on channel after a read timeout. It's inspired by IdleStateHandler but it's
 * specialized version, just flushing data after no read is done on the channel after a period. It's
 * used to avoid aggressively flushing from the ProtocolProcessor.
 *
 * @author liangjiajun
 */
public class AutoFlushHandler extends ChannelDuplexHandler implements IHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AutoFlushHandler.class);
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long writerIdleTimeNanos;
    volatile ScheduledFuture<?> writerIdleTimeout;
    volatile long lastWriteTime;
    // private boolean firstWriterIdleEvent = true;

    /**
     * 0 - none, 1 - initialized, 2 - destroyed
     */
    private volatile STATE state = STATE.none;

    public AutoFlushHandler(long writerIdleTime, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet. this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
//        if (ctx.channel().isActive()) {
//            initialize(ctx);
//        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired. If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
//        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    private void initialize(ChannelHandlerContext ctx) {
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing autoflush handler on channel {} Cid: {}", ctx.channel(),
                    getClientId(ctx.channel()));
        }
        if (state.equals(STATE.initialized) || state.equals(STATE.destroyed)) {
            return;
        }

        state = STATE.initialized;

        EventExecutor loop = ctx.executor();

        lastWriteTime = System.nanoTime();
        writerIdleTimeout = loop.schedule(new WriterIdleTimeoutTask(ctx), writerIdleTimeNanos, TimeUnit.NANOSECONDS);
    }

    private void destroy() {
        state = STATE.destroyed;

        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
    }

    /**
     * Is called when the write timeout expire.
     *
     * @param ctx the channel context.
     */
    private void channelIdle(ChannelHandlerContext ctx) {
        // ctx.fireUserEventTriggered(evt);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Flushing idle Netty channel {} Cid: {}", ctx.channel(), getClientId(ctx.channel()));
        }
        ctx.channel().flush();
    }

    private final class WriterIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            // long lastWriteTime = IdleStateHandler.this.lastWriteTime;
            // long lastWriteTime = lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (System.nanoTime() - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = ctx.executor().schedule(this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                try {
                    /*
                     * IdleStateEvent event; if (firstWriterIdleEvent) { firstWriterIdleEvent =
                     * false; event = IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT; } else { event =
                     * IdleStateEvent.WRITER_IDLE_STATE_EVENT; }
                     */
                    channelIdle(ctx/* , event */);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = ctx.executor().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private enum STATE {
        // 0 - none, 1 - initialized, 2 - destroyed
        none, initialized, destroyed
    }
}

