/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;

/**
 * The interface declares methods to attach/detach one IInputChannel.
 * 
 * The input channel is used in one-way communication by a listener to receive messages.
 * Components that are able to attach the input channel can listen to messages but they cannot send back any response message.
 * 
 */
public interface IAttachableInputChannel
{
	/**
	 * Attaches the input channel.
	 * It stores the reference to the input channel and starts the listening.
	 * 
	 * @param inputChannel
	 * 
	 * @see IInputChannel
	 */
	void attachInputChannel(IInputChannel inputChannel) throws Exception;
	
	
	/**
	 * Detaches the input channel.
	 * It cleans the reference to the input channel and stops the listening.
	 * 
	 */
	void detachInputChannel() throws Exception;
	

	/**
	 * Returns true if the reference to the input channel is stored.
	 * 
	 * Notice, unlike version 1.0, the true value does not mean the channel is listening.
	 * The method {@link IAttachableInputChannel.attachInputChannel} starts also the listening, but if the listening stops (for whatever reason),
	 * the input channel stays attached. To figure out if the input channel is listening use property {@link IInputChannel.IsListening}.
	 * 
	 * @return
	 */
	boolean isInputChannelAttached();
	

	/**
	 * Returns attached input channel.
	 * 
	 * @see IInputChannel
	 */
	IInputChannel getAttachedInputChannel();
}
