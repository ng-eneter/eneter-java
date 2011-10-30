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
 * Declares the input channel that can receive messages from the output channel.
 */
public interface IInputChannel
{
    /**
     * The event is invoked when a message was received.
     */
    Event<ChannelMessageEventArgs> messageReceived();

    /**
     * Returns id of the channel.
     * The channel id represents the address the receiver is listening to.
     */
    String getChannelId();

    /**
     * Starts listening.
     */
    void startListening();

    /**
     * Stops listening.
     */
    void stopListening();

    /**
     * Returns true if the input channel is listening.
     */
    boolean isListening();
}
