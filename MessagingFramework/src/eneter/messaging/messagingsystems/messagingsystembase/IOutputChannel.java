/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Declares the output channel that can send messages to the input channel.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public interface IOutputChannel {
	/**
	 * Returns the id of the input channel where messages are sent.
	 */
    String getChannelId();

    /**
     * Sends the message.
     * 
     * @param message Serialized message.
     * @throws Exception The implementation should catch and trace all problems and then rethrow them.
     */
    void sendMessage(Object message) throws Exception;
}
