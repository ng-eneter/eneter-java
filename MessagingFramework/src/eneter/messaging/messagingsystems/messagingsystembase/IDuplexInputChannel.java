/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

import eneter.messaging.threading.dispatching.IDispatcher;
import eneter.net.system.Event;

/**
 * Declares the duplex input channel that can receive messages from the duplex output channel and send back response messages.
 * 
 */
public interface IDuplexInputChannel
{
    /**
     * The event is invoked before a duplex output channel opens the connection.
     * 
     * The event allows to grant or deny the connection.
     * E.g. if the IsConnectionAllowed is set to false the connection will not be open.<br/>
     * This event does not use the dispatcher. Therefore the event can be raised in whatever thread.
     */
    Event<ConnectionTokenEventArgs> responseReceiverConnecting();

    /**
     * The event is invoked when a duplex output channel opened the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when a duplex output channel closed the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * The event is invoked when a message was received.
     */
    Event<DuplexChannelMessageEventArgs> messageReceived();
    
    /**
     * Returns id of this duplex input channel.
     * The id represents the 'address' the duplex input channel is listening to.
     */
    String getChannelId();

    /**
     * Starts listening to messages.
     * @throws Exception The implementation should catch and trace all problems and then rethrow them.
     */
    void startListening() throws Exception;

    /**
     * Stops listening to messages.
     */
    void stopListening();

    /**
     * Returns true if the duplex input channel is listening.
     */
    boolean isListening() throws Exception;

    /**
     * Sends the response message back to the connected IDuplexOutputChannel.
     * @param responseReceiverId Identifies the response receiver. The identifier comes with received messages.
     * @param message response message
     * @throws Exception The implementation should catch and trace all problems and then rethrow them.
     */
    void sendResponseMessage(String responseReceiverId, Object message) throws Exception;

    /**
     * Disconnects the response receiver.
     * @param responseReceiverId response receiver to be disconnected.
     * @throws Exception 
     */
    void disconnectResponseReceiver(String responseReceiverId) throws Exception;
    
    /**
     * Returns dispatcher that defines the threading model for raising events.
     * Dispatcher is responsible for raising ConnectionOpened, ConnectionClosed and ResponseMessageReceived events
     * in desired thread. It allows to specify which threading mechanism/model is used to raise asynchronous events.
     * E.g. events are queued and raised by one thread. Or e.g. in Silverlight events can be raised in the Silverlight thread.
     */
    IDispatcher getDispatcher();
}
