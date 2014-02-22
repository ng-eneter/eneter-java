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
 * Duplex input channel that can receive messages from the duplex output channel and send response messages.
 * 
 */
public interface IDuplexInputChannel
{
    /**
     * The event is invoked when a duplex output channel opened the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when a duplex output channel closed the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * The event is invoked when a message is received.
     */
    Event<DuplexChannelMessageEventArgs> messageReceived();
    
    /**
     * Returns id of this duplex input channel.
     * The id represents the 'address' the duplex input channel is listening to.
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
     * Returns true if the duplex input channel is listening.
     */
    boolean isListening();

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
     * 
     * Dispatcher is responsible for raising ResponseReceiverConnected, ResponseReceiverDisconnected and MessageReceived events
     * according to desired thread model.
     * E.g. events are queued and raised by one particular thread.
     */
    IThreadDispatcher getDispatcher();
}
