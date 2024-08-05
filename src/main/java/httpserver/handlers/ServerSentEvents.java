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

public class ServerSentEvents implements HttpHandler {

    private final Timer timer = new Timer();
    private final List<PrintWriter> connections = new ArrayList<>();

    public ServerSentEvents() {
        this(MINUTES.toMillis(1));
    }
    public ServerSentEvents(final long keepAliveMillis) {
        if (keepAliveMillis < 1000)
            throw new IllegalArgumentException("Set a reasonable time between keep-alive messages; x >= 1000");
        this.timer.scheduleAtFixedRate(newKeepAliveTask(connections), keepAliveMillis, keepAliveMillis);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(OK);
        exchange.setResponseHeader(CACHE_CONTROL, "no-cache");
        exchange.setResponseHeader(CONNECTION, "keep-alive");
        exchange.setResponseHeader(CONTENT_TYPE, "text/event-stream");
        exchange.setNoContentLength();
        exchange.sendResponse();

        synchronized (connections) {
            connections.add(new PrintWriter(exchange.getOutputStream()));
        }
    }

    public void sendMessage(final String message) {
        sendMessage(null, message);
    }
    public void sendMessage(final String id, final String message) {
        final var event = newSseEvent(id, null, message);

    }

    private static TimerTask newKeepAliveTask(final List<PrintWriter> connections) {
        return new TimerTask() {
            public void run() {
                synchronized (connections) {
                    final var it = connections.iterator();
                    while (it.hasNext()) {
                        final var connection = it.next();
                        final var event = newKeepAliveEvent();
                        try {
                            connection.print(event);
                        } catch (Exception e) {
                            it.remove();
                        }
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

    private static String newSseEvent(final String id, final String type, final String data) {
        final StringBuilder message = new StringBuilder();
        message.append("id: ").append(id == null ? currentTimeMillis() : id).append("\n");
        if (type != null) message.append("event: ").append("\n");
        if (data != null) message.append("data: ").append(data).append("\n");
        return message.append("\n").toString();
    }

}
