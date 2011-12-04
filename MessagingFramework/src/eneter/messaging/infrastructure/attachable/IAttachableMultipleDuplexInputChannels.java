/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

/**
 * The interface declares methods to attach/detach multiple IDuplexInputChannel.
 * 
 * Some comunication components need to attach several channels. E.g. {@link IDuplexDispatcher}.
 * Component using multiple duplex input channels is used in request-response communication and is
 * able to listen to requests on more channels (addresses) and send back (to the right sender) the response message.
 */
public interface IAttachableMultipleDuplexInputChannels
{
	/**
	 * Attaches the duplex input channel nad starts listening to messages.
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
	void detachDuplexInputChannel() throws Exception;

	
	/**
	 * Returns true if the duplex input channel is attached.
	 */
	boolean isDuplexInputChannelAttached();
	

	/**
	 * Detaches the duplex input channel.
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
