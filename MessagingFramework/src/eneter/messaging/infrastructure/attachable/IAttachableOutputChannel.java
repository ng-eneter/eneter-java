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
 * The interface declares methods to attach/detach one {@link IOutputChannel}.
 * 
 * The output channel is used in one-way communication by a sender to send messages.
 * Components that are able to attach the output channel can send messages but they cannot receive any response messages.
 */
public interface IAttachableOutputChannel
{
	/**
	 * Attaches the output channel.
	 * 
	 * @param outputChannel
	 * 
	 * @see IOutputChannel
	 */
	void attachOutputChannel(IOutputChannel outputChannel);
	

	/**
	 * Detaches the output channel.
	 */
	void detachOutputChannel();
	

	/**
	 * Returns true if the output channel is attached.
	 * 
	 * If the output channel is attached it means the object that has attached the channel
	 * can send messages.
	 */
	boolean isOutputChannelAttached();
	

	/**
	 * Returns attached output channel.
	 * 
	 * @see IOutputChannel
	 */
	IOutputChannel getAttachedOutputChannel();
}
