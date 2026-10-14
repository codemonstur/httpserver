# Websockets

httpserver speaks the websocket protocol (RFC 6455) directly — it is part of the server, not
an extension. A websocket connection costs one worker for its lifetime, exactly like server
sent events: the connection is taken over after the handshake and is never read as HTTP
again.

## Using

Check for the upgrade, accept, and answer messages:

```java
final HttpHandler chat = exchange -> {
    if (!exchange.isWebSocketUpgrade()) {
        exchange.setStatusCode(StatusCode.BAD_REQUEST);
        exchange.send("Expected a websocket upgrade", UTF_8);
        return;
    }
    exchange.acceptWebSocket(new WebSocketHandler() {
        @Override public void onMessage(final WebSocket webSocket, final String message) throws Exception {
            webSocket.send("you said: " + message);
        }
        @Override public void onClose(final WebSocket webSocket, final int code, final String reason) {
            System.out.println("closed: " + code + " " + reason);
        }
    });
};
```

`acceptWebSocket` blocks until the session ends, so routing, compression wrappers and
handlers around it all compose the way they do for any other handler.

## API

`HttpServerExchange`:

| method | meaning |
|---|---|
| `isWebSocketUpgrade()` | the client asked to upgrade (checks `Upgrade` and `Connection` tokens, lists included) |
| `acceptWebSocket(handler)` | completes the handshake and runs the session; returns the closed session, or null when the handshake was rejected |

`WebSocketHandler` — every callback runs on the connection's worker, one message at a time;
all of them have empty defaults, so override only what you need:

| callback | meaning |
|---|---|
| `onOpen(webSocket)` | after the handshake, before the first read; a good place for a greeting |
| `onMessage(webSocket, String)` | a complete text message |
| `onMessage(webSocket, byte[])` | a complete binary message |
| `onClose(webSocket, code, reason)` | the session ended, with the peer's close status (1005 when it hung up without one); called exactly once |

`WebSocket` — the sending half works from any thread; frames from concurrent senders are
serialized, never interleaved:

| method | meaning |
|---|---|
| `send(String)` / `send(byte[])` | a text or binary message |
| `sendPing()` / `sendPing(byte[])` | the protocol's own keepalive |
| `close()` / `close(code, reason)` | ends the session; the close frame goes out unless the peer already started the closing handshake |
| `isOpen()` | false once the session is ending or ended |

## Behavior

- **Handshake**: only version 13 is accepted; anything else is answered `426 Upgrade
  Required` with `Sec-WebSocket-Version: 13`, and a missing or malformed
  `Sec-WebSocket-Key` is answered `400`. A rejected handshake leaves the connection an
  ordinary keep-alive HTTP connection.
- **Framing**: client frames must be masked; server frames never are. Reserved bits,
  unknown opcodes, fragmented control frames and out-of-range close codes fail the
  connection with a close frame carrying the right status (1002, or 1009 for oversized).
- **Messages**: fragmented messages are reassembled before the handler sees them, and
  control frames may arrive between the fragments. Pings are answered with a pong
  automatically. A handler that throws takes the connection down with a 1011 close
  instead of leaving the peer waiting.
- **Limits**: frames are capped at 16 MiB (`Frames.MAX_PAYLOAD`), and so is a reassembled
  message. Control frames keep the spec's 125 byte limit.
- **Subprotocols** are not negotiated for you: set the `Sec-WebSocket-Protocol` response
  header yourself before calling `acceptWebSocket` if you need them.

## Not included

No permessage-deflate or other extensions: RSV bits on an incoming frame are a protocol
error, by the book. Compression stays where the rest of the server keeps it — at the HTTP
layer, before a websocket ever begins.
