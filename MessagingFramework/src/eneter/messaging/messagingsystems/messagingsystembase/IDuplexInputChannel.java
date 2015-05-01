/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.Event;

/**
 * Accepts connections from output channels and receives/sends messages from/to connected output channels.
 * 
 */
public interface IDuplexInputChannel
{
    /**
     * The event is raised when an output channel opened the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is raised when an output channel closed the connection.
     * The event is not raised when the connection was closed by the input channel.
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * The event is raised when a message is received.
     */
    Event<DuplexChannelMessageEventArgs> messageReceived();
    
    /**
     * Returns address of the input channel.
     */
    String getChannelId();

    /**
     * Starts listening to messages.
     * @throws Exception
     */
    void startListening() throws Exception;

    /**
     * Stops listening to messages.
     */
    void stopListening();

    /**
     * Returns true if the input channel is listening.
     */
    boolean isListening();

    /**
     * Sends the message back to the connected IDuplexOutputChannel.
     * 
     * The following example shows how to send a broadcast message to all connected output channels:
     * <pre>
     * {@code
     * ...
     * anInputChannel.sendResponseMessage("*", "Hello.");
     * ...
     * }
     * </pre>
     * 
     * @param responseReceiverId Identifies the response receiver. The identifier comes with received messages.
     *                           If the identifier is * then the broadcast message to all connected output channels is sent. 
     * @param message response message
     * @throws Exception
     * 
     * 
     */
    void sendResponseMessage(String responseReceiverId, Object message) throws Exception;

    /**
     * Disconnects the response receiver.
     * @param responseReceiverId response receiver to be disconnected.
     * @throws Exception 
     */
    void disconnectResponseReceiver(String responseReceiverId);
    
    /**
     * Returns dispatcher that defines the threading model for received messages and raised events.
     * 
     * Dispatcher provides the threading model for responseReceiverConnected, responseReceiverDisconnected and messageReceived events.
     * E.g. you can specify that received messages and raised events invoked always in one particular thread. 
     */
    IThreadDispatcher getDispatcher();
}
