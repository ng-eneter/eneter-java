/**
 * 
 */
package net.eneter.messaging.messagingsystems.messagingsystembase;

import org.perfectjpattern.core.api.behavioral.observer.*;

/**
 * The interface declares the API for the input channel.
 * The input channel is able to receive messages. (one-way communication)
 *
 * @author vachix
 *
 */
public interface IInputChannel {
	/**
	 * The event is invoked when a message was received.
	 */
	ISubject<ChannelMessageEventArgs> MessageReceived();

	/**
	 * Returns id of the channel.
     * The channel id represents the address the receiver is listening to
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
     * @return
     */
    boolean GetIsListening();
}
