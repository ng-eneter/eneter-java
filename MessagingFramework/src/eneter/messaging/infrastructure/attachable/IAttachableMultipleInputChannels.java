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
 * The interface declares methods to attach/detach multiple IInputChannel.
 *
 * Some comunication components need to attach several channels.
 * Components using multiple input channels are used in one-way communication.
 * They are able to listen to messages on more input channels (addresses).
 * But they are not able to send back response messages.
 * 
 * @see IDispatcher
 */
public interface IAttachableMultipleInputChannels
{	
	/**
	 * Attaches the input channel.
	 * It stores the reference to the input channel and starts the listening.
	 * 
	 * @param inputChannel
	 * @throws Exception 
	 * 
	 * @see IInputChannel
	 */
	void attachInputChannel(IInputChannel inputChannel) throws Exception;
	

	/**
	 * Detaches the input channel.
	 * It cleans the reference to the input channel and stops the listening.
	 */
	void detachInputChannel() throws Exception;
	
	
	/**
	 * Detaches the input channel.
	 * 
	 * @param channelId
	 * @throws Exception 
	 */
	void detachInputChannel(String channelId) throws Exception;
	

	/**
	 * Returns true if the reference to the input channel is stored.
	 * 
	 * Notice, unlike version 1.0, the true value does not mean the channel is listening.
	 * The method {@link IAttachableMultipleInputChannels.attachInputChannel} starts also the listening, but if the listening stops (for whatever reason),
	 * the input channel stays attached. To figure out if the input channel is listening use property {@link IInputChannel.isListening}.
	 * 
	 * @return
	 */
	boolean isInputChannelAttached();
	
	
	/**
	 * Returns attached input channels.
	 * 
	 * @see IInputChannel
	 */
	Iterable<IInputChannel> getAttachedInputChannels();
}
