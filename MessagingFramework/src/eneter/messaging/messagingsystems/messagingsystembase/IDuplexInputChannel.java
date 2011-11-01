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
 * Declares the duplex input channel that can receive messages from the duplex output channel and send back response messages.
 * Notice, the duplex input channel works only with duplex output channel and not with output channel.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public interface IDuplexInputChannel
{
    /**
     * The event is invoked when a message was received.
     */
    Event<DuplexChannelMessageEventArgs> messageReceived();

    /**
     * The event is invoked when a duplex output channel opened the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when a duplex output channel closed the connection.
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

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
    Boolean isListening();

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
     */
    void disconnectResponseReceiver(String responseReceiverId);
}
