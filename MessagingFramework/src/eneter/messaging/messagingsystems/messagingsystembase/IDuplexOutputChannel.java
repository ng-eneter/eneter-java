/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

import eneter.net.system.Event;

/**
 * Declares the duplex output channel that can send messages to the duplex input channel and receive response messages.
 * Notice, the duplex output channel works only with duplex input channel and not with input channel.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public interface IDuplexOutputChannel
{
    /**
     * The event is invoked when a response message was received.
     * Notice, this event is invoked in a different thread. The exception is only the Synchronous messaging that
     * invokes this event in the thread calling the method SendResponseMessage.
     */
    Event<DuplexChannelMessageEventArgs> responseMessageReceived();
    
    /**
     * The event is invoked when the connection with the duplex input channel was opened.
     * Notice, the event is invoked in a thread from the thread pool.
     */
    Event<DuplexChannelEventArgs> connectionOpened();

    /**
     * The event is invoked when the connection with the duplex input channel was closed.
     * Notice, the event is invoked in a thread from the thread pool.
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    
    /**
     * Returns the id of the duplex input channel where messages are sent.
     * It represents the address where messages are sent.
     * 
     * The channel id represents the communication address. The syntax of the channel id depends on the chosen
     * communication. If the messaging is based on http, the address would be e.g.: http://127.0.0.1/Something/ or
     * http://127.0.0.1:7345/Something/. If the communication is based on tcp, the address would be e.g.: tcp://127.0.0.1:7435/.
     */
    String getChannelId();
    
    /**
     * Returns response receiving id of the duplex output channel.
     * The response receiver id is a unique identifier used by the duplex input channel to recognize
     * connected duplex output channels.
     */
    String getResponseReceiverId();
    
    /**
     * Sends the message to the address represented by ChannelId.
     */
    void sendMessage(Object message);
    
    /**
     * Opens the connection with the duplex input channel.
     */
    void openConnection();
    
    /**
     * Closes the connection with the duplex input channel.
     */
    void closeConnection();
    
    /**
     * Returns true if the duplex output channel is connected to the duplex input channel and listens to response messages.
     */
    Boolean isConnected();
}
