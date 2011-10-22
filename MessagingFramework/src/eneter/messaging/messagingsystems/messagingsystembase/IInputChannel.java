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
    Event<ChannelMessageEventArgs> MessageReceived();

    /**
     * Returns id of the channel.
     * The channel id represents the address the receiver is listening to.
     */
    String GetChannelId();

    /**
     * Starts listening.
     */
    void StartListening();

    /**
     * Stops listening.
     */
    void StopListening();

    /**
     * Returns true if the input channel is listening.
     */
    boolean IsListening();
}
