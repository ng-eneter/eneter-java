/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Declares the output channel that can send messages to the input channel.
 */
public interface IOutputChannel {
	/**
	 * Returns the id of the input channel where messages are sent.
	 */
    String GetChannelId();

    /**
     * Sends the message.
     * 
     * @param message Serialized message.
     */
    void SendMessage(Object message);
}
