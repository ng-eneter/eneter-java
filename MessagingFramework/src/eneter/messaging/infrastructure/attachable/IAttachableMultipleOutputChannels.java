/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

/**
 * The interface declares methods to attach/detach multiple {@link IOutputChannel}.
 *
 * Some comunication components need to attach several channels.
 * Components using multiple output channels are used in one-way communication.
 * They are able to send messages to several receivers. E.g. {@link IDispatcher}
 */
public interface IAttachableMultipleOutputChannels
{
	/**
	 * Attaches the output channel.
	 * @throws Exception 
	 * 
	 * @see IOutputChannel
	 */
	void attachOutputChannel(IOutputChannel outputChannel) throws Exception;
	

	/**
	 * Detaches all output channels.
	 */
	void detachOutputChannel();
	

	/**
	 * Detaches the output channel.
	 * 
	 * @param channelId
	 * @throws Exception 
	 */
	void detachOutputChannel(String channelId) throws Exception;
	

	/**
	 * Returns true if at least one output channel is attached.
	 * 
	 * If the output channel is attached it means the object that has attached the channel
	 * can send messages.
	 * Multiple output channel attachable means that the object can send messages to more receivers.
	 */
	boolean isOutputChannelAttached();
	

	/**
	 * Returns attached output channels.
	 * 
	 * @see IOutputChannel
	 */
	Iterable<IOutputChannel> getAttachedOutputChannels();
}
