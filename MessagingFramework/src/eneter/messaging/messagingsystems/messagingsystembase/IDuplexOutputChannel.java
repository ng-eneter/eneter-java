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
 * Declares the duplex output channel that can send messages to the duplex input channel and receive response messages.
 * 
 */
public interface IDuplexOutputChannel
{
    /**
     * The event is invoked when a response message was received.
     */
    Event<DuplexChannelMessageEventArgs> responseMessageReceived();
    
    /**
     * The event is invoked when the connection with the duplex input channel was opened.
     */
    Event<DuplexChannelEventArgs> connectionOpened();

    /**
     * The event is invoked when the connection with the duplex input channel was closed.
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
     * Sends the message to the duplex input channel.
     * @throws Exception The implementation should catch and trace all problems and then rethrow them.
     */
    void sendMessage(Object message) throws Exception;
    
    /**
     * Opens the connection with the duplex input channel.
     * @throws Exception The implementation should catch and trace all problems and then rethrow them.
     */
    void openConnection() throws Exception;
    
    /**
     * Closes the connection with the duplex input channel.
     */
    void closeConnection();
    
    /**
     * Returns true if the duplex output channel is connected to the duplex input channel and listens to response messages.
     */
    boolean isConnected();
    
    /**
     * Returns dispatcher that defines the threading model for raising events.
     * 
     * Dispatcher is responsible for raising ConnectionOpened, ConnectionClosed and ResponseMessageReceived events
     * according to desired thread model.
     * E.g. events are queued and raised by one particular thread.
     */
    IThreadDispatcher getDispatcher();
}
