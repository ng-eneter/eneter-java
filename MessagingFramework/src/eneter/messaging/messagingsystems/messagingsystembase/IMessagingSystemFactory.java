/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The interface declares the factory that creates input and output channels.
 * This factory interface is supposed to be implemented by particular messaging systems so that
 * a custom implementation for input and output channels can be provided.
 *
 * @author vachix
 *
 */
public interface IMessagingSystemFactory
{
	/**
	 * Creates the output channel sending messages to specified input channel.
	 * 
	 * @param channelId identifies the receiving input channel
	 * @return output channel
	 */
    IOutputChannel createOutputChannel(String channelId);

    /**
     * Creates the input channel listening to messages on the specified channel id.
     * 
     * @param channelId identifies this input channel
     * @return input channel
     */
    IInputChannel createInputChannel(String channelId);
}