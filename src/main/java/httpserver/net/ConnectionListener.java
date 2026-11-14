package httpserver.net;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * The {@code ConnectionListener} class.
 */
public abstract class ConnectionListener {

    /**
     * The {@code backlog} field.
     */
    protected final int backlog;
    /**
     * The {@code handler} field.
     */
    protected final ConnectionHandler handler;
    /**
     * The {@code port} field.
     */
    protected final int port;
    /**
     * The {@code bindAddress} field.
     */
    protected final InetAddress bindAddress;
    /**
     * The {@code daemon} field.
     */
    protected final boolean daemon;
    /**
     * The {@code executor} field.
     */
    protected final Executor executor;

    /**
     * The {@code running} field.
     */
    protected volatile boolean running = true;
    /**
     * The {@code serverChannel} field.
     */
    protected ServerSocketChannel serverChannel;
    /**
     * The {@code selector} field.
     */
    protected Selector selector;
    /**
     * The {@code selectorThread} field.
     */
    protected Thread selectorThread;
    /**
     * The {@code connections} field.
     */
    protected final Set<SocketChannel> connections = ConcurrentHashMap.newKeySet();
    /**
     * The {@code pendingRegistrations} field.
     */
    protected final ConcurrentLinkedQueue<SocketChannel> pendingRegistrations = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new instance.
     *
     * @param port the port
     * @param bindAddress the bind address
     * @param daemon the daemon
     * @param backlog the backlog
     * @param executor the executor
     * @param handler the handler
     */
    protected ConnectionListener(final int port, final InetAddress bindAddress, final boolean daemon,
                                 final int backlog, final Executor executor, final ConnectionHandler handler) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.daemon = daemon;
        this.backlog = backlog;
        this.executor = executor;
        this.handler = handler;
    }

    /**
     * Returns the result of {@code start}.
     *
     * @return the result
     * @throws IOException if the operation fails
     */
    public ConnectionListener start() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(bindAddress, port), backlog);
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        selectorThread = new Thread(this::runSelector, "httpserver-selector");
        selectorThread.setDaemon(daemon);
        selectorThread.start();

        return this;
    }

    private void runSelector() {
        while (running) {
            try {
                // Process channels handed back by workers (on the selector thread only).
                SocketChannel pending;
                while ((pending = pendingRegistrations.poll()) != null) {
                    try {
                        if (pending.isOpen() && !pending.isRegistered()) {
                            pending.configureBlocking(false);
                            pending.register(selector, SelectionKey.OP_READ);
                        } else if (pending.isOpen()) {
                            // Still registered: cancel and re-register on next pass.
                            pendingRegistrations.offer(pending);
                        }
                    } catch (final Exception ignored) {
                        try {
                            pending.close();
                        } catch (final IOException ignored2) {}
                        connections.remove(pending);
                    }
                }

                selector.select(50);
                final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                final java.util.List<SocketChannel> toHandle = new java.util.ArrayList<>();
                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        accept();
                    } else if (key.isReadable()) {
                        final SocketChannel channel = (SocketChannel) key.channel();
                        key.cancel();
                        toHandle.add(channel);
                    }
                }
                // Force cancellations to take effect before workers switch to blocking mode.
                try {
                    selector.selectNow();
                } catch (final IOException ignored) {}
                for (final SocketChannel channel : toHandle) {
                    try {
                        executor.execute(() -> handleChannel(channel));
                    } catch (final Exception ignored) {
                        try {
                            channel.close();
                        } catch (final IOException ignored2) {}
                        connections.remove(channel);
                    }
                }
            } catch (final IOException ignored) {
                // Keep the selector alive.
            }
        }

        try {
            if (serverChannel != null) serverChannel.close();
        } catch (final IOException ignored) {}
        for (final SocketChannel channel : connections) {
            try {
                channel.close();
            } catch (final IOException ignored) {}
        }
        connections.clear();
        try {
            if (selector != null) selector.close();
        } catch (final IOException ignored) {}
    }

    private void accept() {
        try {
            SocketChannel channel;
            while ((channel = serverChannel.accept()) != null) {
                channel.configureBlocking(false);
                try {
                    channel.socket().setTcpNoDelay(true);
                } catch (final IOException ignored) {}
                connections.add(channel);
                channel.register(selector, SelectionKey.OP_READ);
            }
        } catch (final IOException ignored) {}
    }

    private void handleChannel(final SocketChannel channel) {
        // Cancellations were flushed on the selector thread (selectNow), so the channel is
        // already unregistered here.
        try {
            channel.configureBlocking(true);
        } catch (final IOException ignored) {
            try {
                channel.close();
            } catch (final IOException ignored2) {}
            connections.remove(channel);
            return;
        }

        boolean close = false;
        BufferedOutputStream out = null;
        try {
            final var socket = channel.socket();
            final var rawIn = socket.getInputStream();
            final var rawOut = socket.getOutputStream();
            // Buffering coalesces the head and body writes into fewer syscalls per response.
            final var in = new BufferedInputStream(rawIn, 8192);
            out = new BufferedOutputStream(rawOut, 8192);
            while (!close) {
                try {
                    handler.handleInput(socket, in, out);
                } catch (final IOException e) {
                    close = true;
                    break;
                }
                // Drain any requests already buffered on this connection.
                try {
                    if (rawIn.available() <= 0 && in.available() <= 0) {
                        // Briefly wait for a follow-up request on a keep-alive connection
                        // to avoid a selector round-trip for back-to-back requests.
                        for (int spin = 0; spin < 128 && rawIn.available() <= 0 && in.available() <= 0; spin++) {
                            Thread.onSpinWait();
                        }
                        if (rawIn.available() <= 0 && in.available() <= 0) break;
                    }
                } catch (final IOException ignored) {
                    break;
                }
            }
        } catch (final Exception ignored) {
            close = true;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (final IOException ignored) {}
            }
            if (!close && channel.isOpen() && !channel.socket().isClosed()) {
                pendingRegistrations.offer(channel);
                selector.wakeup();
            } else {
                try {
                    channel.close();
                } catch (final IOException ignored) {}
                connections.remove(channel);
            }
        }
    }

    /**
     * Performs the {@code stop} operation.
     *
     * @throws IOException if the operation fails
     */
    public void stop() throws IOException {
        running = false;
        try {
            if (selector != null) selector.wakeup();
        } catch (final Exception ignored) {}
        try {
            if (selectorThread != null) selectorThread.join();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the port.
     *
     * @return the result
     */
    public int getPort() {
        try {
            return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
        } catch (final IOException e) {
            return port;
        }
    }

}
