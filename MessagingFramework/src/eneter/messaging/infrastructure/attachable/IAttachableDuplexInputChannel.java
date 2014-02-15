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
 * Declares methods to attach/detach one {@link IDuplexInputChannel}.
 * 
 * Communication components implementing this interface can attach the duplex input channel and
 * receive messages and sends response messages.
 * 
 *
 */
public interface IAttachableDuplexInputChannel
{	
	/**
	 * Attaches the duplex input channel and starts listening to messages.
	 * 
	 * @param duplexInputChannel
	 * @see IDuplexInputChannel
	 */
	void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel) throws Exception;
	

	/**
	 * Detaches the duplex input channel and stops listening to messages.
	 */
	void detachDuplexInputChannel();
	

	/**
	 * Returns true if the duplex input channel is attached.
	 * 
	 * @return
	 */
	boolean isDuplexInputChannelAttached();
	

	/**
	 * Returns attached duplex input channel.
	 * 
	 * @return
	 * 
	 * @see IDuplexInputChannel
	 */
	IDuplexInputChannel getAttachedDuplexInputChannel();
}
