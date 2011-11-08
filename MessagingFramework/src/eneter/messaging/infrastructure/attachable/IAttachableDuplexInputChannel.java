/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

/**
 * The interface declares methods to attach/detach one IDuplexInputChannel.
 * 
 * The duplex input channel is used in the request-response communication by a listener
 * to receive request messages and send back response messages.
 * 
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IAttachableDuplexInputChannel
{	
	/**
	 * Attaches the duplex input channel and starts listening to messages.
	 * 
	 * @param duplexInputChannel
	 * 
	 * @see IDuplexInputChannel
	 */
	void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel);
	

	/**
	 * Detaches the duplex input channel and stops listening to messages.
	 */
	void detachDuplexInputChannel();
	

	/**
	 * Returns true if the duplex input channel is attached.
	 * 
	 * @return
	 */
	Boolean isDuplexInputChannelAttached();
	

	/**
	 * Retutns attached duplex input channel.
	 * 
	 * @return
	 * 
	 * @see IDuplexInputChannel
	 */
	IDuplexInputChannel getAttachedDuplexInputChannel();
}