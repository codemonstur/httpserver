package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static httpserver.core.Headers.*;
import static httpserver.core.StatusCode.OK;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * The {@code ServerSentEvents} class.
 */
public class ServerSentEvents implements HttpHandler {

    private final Timer timer = new Timer("server-sent-events-keep-alive", true);
    private final List<PrintWriter> connections = new ArrayList<>();

    /**
     * Creates a new instance.
     */
    public ServerSentEvents() {
        this(MINUTES.toMillis(1));
    }
    /**
     * Creates a new instance.
     *
     * @param keepAliveMillis the keep alive millis
     */
    public ServerSentEvents(final long keepAliveMillis) {
        if (keepAliveMillis < 1000)
            throw new IllegalArgumentException("Set a reasonable time between keep-alive messages; x >= 1000");
        this.timer.scheduleAtFixedRate(newKeepAliveTask(connections), keepAliveMillis, keepAliveMillis);
    }

    /**
     * Performs the {@code handleRequest} operation.
     *
     * @param exchange the exchange
     * @throws Exception if the operation fails
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(OK);
        exchange.setResponseHeader(CACHE_CONTROL, "no-cache");
        exchange.setResponseHeader(CONNECTION, "keep-alive");
        exchange.setResponseHeader(CONTENT_TYPE, "text/event-stream");
        exchange.setNoContentLength();
        exchange.sendResponse();

        final var connection = new PrintWriter(exchange.getOutputStream());
        synchronized (connections) {
            connections.add(connection);
        }
        try {
            // The connection belongs to this stream now: hold it (and its worker) open until the
            // client leaves, instead of letting the keep-alive loop try to parse whatever the
            // event source sends next as a new request.
            exchange.awaitClientDisconnect();
        } finally {
            synchronized (connections) {
                connections.remove(connection);
            }
        }
    }

    /**
     * Performs the {@code sendMessage} operation.
     *
     * @param message the message
     */
    public void sendMessage(final String message) {
        sendMessage(null, null, message);
    }
    /**
     * Performs the {@code sendMessage} operation.
     *
     * @param id the id
     * @param message the message
     */
    public void sendMessage(final String id, final String message) {
        sendMessage(id, null, message);
    }
    /**
     * Performs the {@code sendMessage} operation.
     *
     * @param id the id
     * @param type the type
     * @param message the message
     */
    public void sendMessage(final String id, final String type, final String message) {
        broadcast(newSseEvent(id, type, message));
    }

    private void broadcast(final String event) {
        synchronized (connections) {
            final var it = connections.iterator();
            while (it.hasNext()) {
                final var connection = it.next();
                connection.print(event);
                connection.flush();
                // PrintWriter never throws, so its error flag is the only way to notice a client
                // that went away without us noticing, and to stop accumulating dead writers.
                if (connection.checkError()) it.remove();
            }
        }
    }

    private static TimerTask newKeepAliveTask(final List<PrintWriter> connections) {
        return new TimerTask() {
            public void run() {
                synchronized (connections) {
                    final var it = connections.iterator();
                    while (it.hasNext()) {
                        final var connection = it.next();
                        connection.print(newKeepAliveEvent());
                        connection.flush();
                        if (connection.checkError()) it.remove();
                    }
                }
            }
        };
    }
    private static String newKeepAliveEvent() {
        return "id: " + currentTimeMillis() + "\n"
             + "event: keep-alive\n"
             + "\n";
    }

    /**
     * Every line of the payload has to be its own data field, otherwise the client only sees the
     * first one and drops the rest of the message.
     */
    private static String newSseEvent(final String id, final String type, final String data) {
        final StringBuilder message = new StringBuilder();
        message.append("id: ").append(id == null ? currentTimeMillis() : id).append("\n");
        if (type != null) message.append("event: ").append(type).append("\n");
        if (data != null)
            for (final String line : data.split("\r\n|\n|\r"))
                message.append("data: ").append(line).append("\n");
        return message.append("\n").toString();
    }

}
