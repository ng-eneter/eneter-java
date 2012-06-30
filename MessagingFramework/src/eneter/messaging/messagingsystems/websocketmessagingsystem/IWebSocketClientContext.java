/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;
import java.util.Map;

import eneter.net.system.Event;

/**
 * Represents the client context on the server side.
 * The client context is obtained when a client opened the connection with the server and
 * it provides functionality to receive messages from the client and send back response messages.
 * To see the example refer to {@link WebSocketListener}.
 */
public interface IWebSocketClientContext
{
    /**
     * The event is invoked when the connection with the client was closed.
     * @return
     */
    Event<Object> connectionClosed();
    
    /**
     * The event is invoked when the pong message was received.
     * @return
     */
    Event<Object> pongReceived();
    
    /**
     * Returns true if the client is connected.
     * @return
     */
    boolean isConnected();
    
    /**
     * Returns URI of this connection including query parameters sent from by the client.
     * @return
     */
    URI getUri();
    
    /**
     * Returns the readonly dictionary.
     * @return
     */
    Map<String, String> getHeaderFields();
    
    /**
     * Sends message to the client.
     * The message must be type of string or byte[]. If the type is string then the message is sent as the text message via text frame.
     * If the type is byte[] the message is sent as the binary message via binary frame.
     * 
     * @param data message to be sent to the client. Must be byte[] or string.
     * @throws Exception If sending of the message failed.
     */
    void sendMessage(Object data) throws Exception;
    
    /**
     * Sends message to the client. Allows to send the message via multiple frames.
     * The message must be type of string or byte[]. If the type is string then the message is sent as the text message via text frame.
     * If the type is byte[] the message is sent as the binary message via binary frame.<br/>
     * It allows to send the message in multiple frames. The client then can receive all parts separately
     * using WebSocketMessage.InputStream or as a whole message using WebSocketMessage.GetWholeMessage().
     * <br/>
     * The following example shows how to send 'Hello world.' in three parts.
     * <pre>
     * {@code
     * void ProcessConnection(IWebSocketClientContext clientContext)
     * {
     *     ...
     *     
     *     // Send the first part of the message.
     *     clientContext.sendMessage("Hello ", false);
     *     
     *     // Send the second part.
     *     clientContext.sendMessage("wo", false);
     *     
     *     // Send the third final part.
     *     clientContext.sendMessage("rld.", true);
     *     
     *     ...
     * }
     * }
     * </pre>
     * 
     * @param data message to be sent to the client. The message can be byte[] or string.
     * @param isFinal true if this is the last part of the message.
     * @throws Exception If sending of the message failed.
     */
    void sendMessage(Object data, boolean isFinal) throws Exception;
    
    /**
     * Waits until a message is received from the client.
     * <br/>
     * Example shows how to implement a loop receiving the text messages from the client.
     * <pre>
     * {@code
     * void ProcessConnection(IWebSocketClientContext clientContext)
     * {
     *     // The loop waiting for incoming messages.
     *     // Note: The waiting thread is released when the connection is closed.
     *     WebSocketMessage aWebSocketMessage;
     *     while ((aWebSocketMessage = clientContext.receiveMessage()) != null)
     *     {
     *         if (aWebSocketMessage.isText())
     *         {
     *             // Wait until all data frames are collected
     *             // and return the message.
     *             String aMessage = aWebSocketMessage.getWholeTextMessage();
     *             ...
     *         }
     *     }
     * }
     * }
     * </pre>
     * @return websocket message
     * @throws Exception
     */
    WebSocketMessage receiveMessage() throws Exception;
    
    /**
     * Pings the client. According to websocket protocol, pong should be responded.
     * @throws Exception
     */
    void sendPing() throws Exception;
    
    /**
     * Sends unsolicited pong to the client.
     * @throws Exception
     */
    void sendPong() throws Exception;
    
    /**
     * Closes connection with the client.
     * It sends the close message to the client and closes the underlying tcp connection.
     */
    void closeConnection();
}
