package httpserver.websocket;

/**
 * What a websocket connection does. Every callback runs on the worker that owns the
 * connection, one message at a time; a handler that needs to send on this connection from
 * elsewhere keeps the WebSocket it is handed and calls send on it from any thread.
 */
public interface WebSocketHandler {

    /** After the opening handshake has completed, before the first message is read. 
     * @param webSocket the web socket
     * @throws Exception if the operation fails
     */
    default void onOpen(final WebSocket webSocket) throws Exception {}

    /** A complete text message. 
     * @param webSocket the web socket
     * @param message the message
     * @throws Exception if the operation fails
     */
    default void onMessage(final WebSocket webSocket, final String message) throws Exception {}

    /** A complete binary message. 
     * @param webSocket the web socket
     * @param message the message
     * @throws Exception if the operation fails
     */
    default void onMessage(final WebSocket webSocket, final byte[] message) throws Exception {}

    /**
     * The connection has ended, with the status the peer closed with (1005 when it hung up
     * without sending one) and that status's reason. Called exactly once, whatever the ending.
     
     * @param webSocket the web socket
     * @param code the code
     * @param reason the reason
     * @throws Exception if the operation fails
     */
    default void onClose(final WebSocket webSocket, final int code, final String reason) throws Exception {}

}
