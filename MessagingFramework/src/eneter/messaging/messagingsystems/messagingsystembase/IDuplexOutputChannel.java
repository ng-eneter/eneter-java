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
 * Duplex output channel which can send messages to the duplex input channel and receive response messages.
 * 
 */
public interface IDuplexOutputChannel
{
    /**
     * The event is raised when the connection with the input channel was opened.
     */
    Event<DuplexChannelEventArgs> connectionOpened();

    /**
     * The event is raised when the connection was closed from the input channel or the it was closed due to a broken connection. 
     * The event is not raised if the connection was closed by the output channel by calling closeConnection().
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * The event is raised when a response message was received.
     */
    Event<DuplexChannelMessageEventArgs> responseMessageReceived();
    
    
    /**
     * Returns the address of the input channel.
     * 
     * The channel id represents the communication address. The syntax of the channel id depends on the chosen
     * communication. If the messaging is based on WebSocket the address will look like
     * ws://127.0.0.1:7345/Something/. If the communication is based on tcp the address would be e.g.: tcp://127.0.0.1:7435/.
     */
    String getChannelId();
    
    /**
     * Returns the unique identifier of this output channel.
     */
    String getResponseReceiverId();
    
    /**
     * Sends the message to the input channel.
     * @param message message to be sent. It can be String or byte[] or some other type depending on used protocol formatter. 
     * @throws Exception
     */
    void sendMessage(Object message) throws Exception;
    
    /**
     * Opens the connection with the input channel.
     * @throws Exception
     */
    void openConnection() throws Exception;
    
    /**
     * Closes the connection with the input channel.
     */
    void closeConnection();
    
    /**
     * Returns true if the output channel is connected to the input channel and listens to response messages.
     */
    boolean isConnected();
    
    /**
     * Returns dispatcher which defines the threading model for received messages and raised events.
     * 
     * Dispatcher is responsible for raising connectionOpened, connectionClosed and responseMessageReceived events.
     * E.g. it can ensure all messages and events are invoked in one particular thread.
     */
    IThreadDispatcher getDispatcher();
}
