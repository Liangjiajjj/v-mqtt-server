package com.iot.mqtt.relay.connection;

import com.google.common.annotations.VisibleForTesting;
import com.iot.mqtt.handler.ClientAutoFlushHandler;
import com.iot.mqtt.relay.handler.RelayMessageDecoderHandler;
import com.iot.mqtt.relay.handler.RelayMessageEncoderHandler;
import com.iot.mqtt.util.FutureUtil;
import com.iot.mqtt.util.netty.EventLoopUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.iot.mqtt.util.netty.ChannelFutures.toCompletableFuture;

/**
 * 转发链接池
 *
 * @author liangjiajun
 */
@Slf4j
public class RelayConnectionPool implements Closeable {

    protected final ConcurrentHashMap<InetSocketAddress, ConcurrentMap<Integer, CompletableFuture<RelayConnection>>> pool;

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final int maxConnectionsPerHosts = 64;
    protected final DnsNameResolver dnsResolver;

    public RelayConnectionPool(String username, String password, EventLoopGroup eventLoopGroup) throws Exception {
        this.eventLoopGroup = eventLoopGroup;
        pool = new ConcurrentHashMap<>();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);

        try {
            bootstrap.handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline channelPipeline = channel.pipeline();
                            // Netty心跳机制
                            channelPipeline.addLast("idle", new IdleStateHandler(0, 0, 60));
                            channelPipeline.addLast("autoflush", new ClientAutoFlushHandler(1, TimeUnit.SECONDS));
                            // 自定义协议
                            channelPipeline.addLast("decoder", new RelayMessageDecoderHandler());
                            channelPipeline.addLast("encoder", RelayMessageEncoderHandler.INSTANCE);
                            channelPipeline.addLast("handler", new RelayConnection(username, password, eventLoopGroup));
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to create channel initializer", e);
        }

        this.dnsResolver = new DnsNameResolverBuilder(eventLoopGroup.next()).traceEnabled(true)
                .channelType(EventLoopUtil.getDatagramChannelClass(eventLoopGroup)).build();
    }


    void closeAllConnections() {
        pool.values().forEach(map -> {
            map.values().forEach(future -> {
                if (future.isDone()) {
                    if (!future.isCompletedExceptionally()) {
                        // Connection was already created successfully, the join will not throw any exception
                        future.join().close();
                    } else {
                        // If the future already failed, there's nothing we have to do
                    }
                } else {
                    // The future is still pending: just register to make sure it gets closed if the operation will
                    // succeed
                    future.thenAccept(RelayConnection::close);
                }
            });
        });
    }

    /**
     * Get a connection from the pool.
     * <p>
     * The connection can either be created or be coming from the pool itself.
     * <p>
     * When specifying multiple addresses, the logicalAddress is used as a tag for the broker, while the physicalAddress
     * is where the connection is actually happening.
     * <p>
     * These two addresses can be different when the client is forced to connect through a proxy layer. Essentially, the
     * pool is using the logical address as a way to decide whether to reuse a particular connection.
     *
     * @param logicalAddress  the address to use as the broker tag
     * @param physicalAddress the real address where the TCP connection should be made
     * @return a future that will produce the RelayConnection object
     */
    public CompletableFuture<RelayConnection> getConnection(long md5Key, InetSocketAddress logicalAddress,
                                                            InetSocketAddress physicalAddress) {
        if (maxConnectionsPerHosts == 0) {
            // Disable pooling
            return createConnection(logicalAddress, physicalAddress, -1);
        }

        final int key = signSafeMod(md5Key, maxConnectionsPerHosts);

        return pool.computeIfAbsent(logicalAddress, a -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> createConnection(logicalAddress, physicalAddress, key));
    }

    public CompletableFuture<RelayConnection> getConnection(long md5Key, final InetSocketAddress address) {
        return getConnection(md5Key, address, true);
    }

    public CompletableFuture<RelayConnection> getConnection(long md5Key, final InetSocketAddress address, boolean isCreate) {
        if (isCreate) {
            return getConnection(md5Key, address, address);
        } else {
            final int key = signSafeMod(md5Key, maxConnectionsPerHosts);
            ConcurrentMap<Integer, CompletableFuture<RelayConnection>> futureConcurrentMap = pool.get(address);
            if (Objects.nonNull(futureConcurrentMap)) {
                return futureConcurrentMap.get(key);
            }
            return null;
        }
    }

    private CompletableFuture<RelayConnection> createConnection(InetSocketAddress logicalAddress,
                                                                InetSocketAddress physicalAddress, int connectionKey) {
        if (log.isDebugEnabled()) {
            log.debug("Connection for {} not found in cache", logicalAddress);
        }

        final CompletableFuture<RelayConnection> cnxFuture = new CompletableFuture<>();

        // Trigger async connect to broker
        createConnection(physicalAddress).thenAccept(channel -> {
            log.info("[{}] Connected to server", channel);

            channel.closeFuture().addListener(v -> {
                // Remove connection from pool when it gets closed
                if (log.isDebugEnabled()) {
                    log.debug("Removing closed connection from pool: {}", v);
                }
                cleanupConnection(logicalAddress, connectionKey, cnxFuture);
            });

            // We are connected to broker, but need to wait until the connect/connected handshake is
            // complete
            final RelayConnection cnx = (RelayConnection) channel.pipeline().get("handler");
            if (!channel.isActive() || cnx == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Connection was already closed by the time we got notified", channel);
                }
                cnxFuture.completeExceptionally(new ChannelException("Connection already closed"));
                return;
            }

            if (!logicalAddress.equals(physicalAddress)) {
                // We are connecting through a proxy. We need to set the target broker in the RelayConnection object so that
                // it can be specified when sending the CommandConnect.
                // That phase will happen in the RelayConnection.connectionActive() which will be invoked immediately after
                // this method.
                cnx.setTargetBroker(logicalAddress);
            }

            cnx.setRemoteHostName(physicalAddress.getHostName());
            cnx.connectionFuture().thenRun(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Connection handshake completed", cnx.channel());
                }
                cnxFuture.complete(cnx);
            }).exceptionally(exception -> {
                log.warn("[{}] Connection handshake failed: {}", cnx.channel(), exception.getMessage());
                cnxFuture.completeExceptionally(exception);
                cleanupConnection(logicalAddress, connectionKey, cnxFuture);
                cnx.ctx().close();
                return null;
            });
        }).exceptionally(exception -> {
            eventLoopGroup.execute(() -> {
                log.warn("Failed to open connection to {} : {}", physicalAddress, exception.getMessage());
                cleanupConnection(logicalAddress, connectionKey, cnxFuture);
                cnxFuture.completeExceptionally(exception);
            });
            return null;
        });

        return cnxFuture;
    }

    /**
     * Resolve DNS asynchronously and attempt to connect to any IP address returned by DNS server
     */
    private CompletableFuture<Channel> createConnection(InetSocketAddress unresolvedAddress) {
        int port;
        CompletableFuture<List<InetAddress>> resolvedAddress = null;
        try {
            port = unresolvedAddress.getPort();
            resolvedAddress = resolveName(unresolvedAddress.getHostString());
            return resolvedAddress.thenCompose(
                    inetAddresses -> connectToResolvedAddresses(inetAddresses.iterator(), port,
                            null));
        } catch (Exception e) {
            log.error("Invalid Proxy url {}", unresolvedAddress.getHostString(), e);
            return FutureUtil
                    .failed(new RuntimeException("Invalid url error" + unresolvedAddress.getHostString(), e));
        }
    }

    /**
     * Try to connect to a sequence of IP addresses until a successfull connection can be made, or fail if no address is
     * working
     */
    private CompletableFuture<Channel> connectToResolvedAddresses(Iterator<InetAddress> unresolvedAddresses, int port, InetSocketAddress sniHost) {
        CompletableFuture<Channel> future = new CompletableFuture<>();

        connectToAddress(unresolvedAddresses.next(), port, sniHost).thenAccept(channel -> {
            // Successfully connected to server
            future.complete(channel);
        }).exceptionally(exception -> {
            if (unresolvedAddresses.hasNext()) {
                // Try next IP address
                connectToResolvedAddresses(unresolvedAddresses, port, sniHost).thenAccept(channel -> {
                    future.complete(channel);
                }).exceptionally(ex -> {
                    // This is already unwinding the recursive call
                    future.completeExceptionally(ex);
                    return null;
                });
            } else {
                // Failed to connect to any IP address
                future.completeExceptionally(exception);
            }
            return null;
        });

        return future;
    }

    @VisibleForTesting
    CompletableFuture<List<InetAddress>> resolveName(String hostname) {
        CompletableFuture<List<InetAddress>> future = new CompletableFuture<>();
        dnsResolver.resolveAll(hostname).addListener((Future<List<InetAddress>> resolveFuture) -> {
            if (resolveFuture.isSuccess()) {
                future.complete(resolveFuture.get());
            } else {
                future.completeExceptionally(resolveFuture.cause());
            }
        });
        return future;
    }

    /**
     * Attempt to establish a TCP connection to an already resolved single IP address
     */
    private CompletableFuture<Channel> connectToAddress(InetAddress ipAddress, int port, InetSocketAddress sniHost) {
        InetSocketAddress remoteAddress = new InetSocketAddress(ipAddress, port);
        return toCompletableFuture(bootstrap.connect(remoteAddress));
    }

    public void releaseConnection(RelayConnection cnx) {
        if (maxConnectionsPerHosts == 0) {
            //Disable pooling
            if (cnx.channel().isActive()) {
                if (log.isDebugEnabled()) {
                    log.debug("close connection due to pooling disabled.");
                }
                cnx.close();
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            eventLoopGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS).await();
        } catch (InterruptedException e) {
            log.warn("EventLoopGroup shutdown was interrupted", e);
        }
        dnsResolver.close();
    }

    private void cleanupConnection(InetSocketAddress address, int connectionKey,
                                   CompletableFuture<RelayConnection> connectionFuture) {
        ConcurrentMap<Integer, CompletableFuture<RelayConnection>> map = pool.get(address);
        if (map != null) {
            map.remove(connectionKey, connectionFuture);
        }
    }

    @VisibleForTesting
    int getPoolSize() {
        return pool.values().stream().mapToInt(Map::size).sum();
    }

    public static int signSafeMod(long dividend, int divisor) {
        int mod = (int) (dividend % divisor);
        if (mod < 0) {
            mod += divisor;
        }
        return mod;
    }

}
