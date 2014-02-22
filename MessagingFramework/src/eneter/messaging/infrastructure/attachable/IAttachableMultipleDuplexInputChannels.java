/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

/**
 * Interface for components which want to attach multiple {@link IDuplexInputChannel}.
 * 
 * Communication components implementing this interface can attach multiple duplex input channels and listens via them to messages.
 */
public interface IAttachableMultipleDuplexInputChannels
{
	/**
	 * Attaches the duplex input channel and starts listening to messages.
	 * 
	 * @param duplexInputChannel
	 * @throws Exception 
	 * @see IDuplexInputChannel
	 */
	void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel) throws Exception;
	

	/**
	 * Detaches the duplex input channel.
	 * Detaching the input channel stops listening to the messages.
	 */
	void detachDuplexInputChannel();

	
	/**
	 * Returns true if the duplex input channel is attached.
	 */
	boolean isDuplexInputChannelAttached();
	

	/**
	 * Detaches the duplex input channel.
	 * 
	 * Detaching the input channel stops listening to the messages.
	 * It releases listening threads.
	 * 
	 * @param channelId
	 * @throws Exception 
	 */
	void detachDuplexInputChannel(String channelId) throws Exception;
	

	/**
	 * Returns attached input channels.
	 * 
	 * @see IDuplexInputChannel
	 */
	Iterable<IDuplexInputChannel> getAttachedDuplexInputChannels();
}
