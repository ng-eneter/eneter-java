/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;

/**
 * Interface for components which want to attach one {@link IDuplexOutputChannel}.
 * 
 * Communication components implementing this interface can attach the duplex output channel and
 * sends messages and receive response messages.
 * 
 *
 */
public interface IAttachableDuplexOutputChannel
{
	/**
	 * Attaches the duplex output channel and opens the connection and starts listening to response messages.
	 * 
	 * @param duplexOutputChannel Duplex output channel to be attached.
	 * @throws Exception 
	 * 
	 * @see IDuplexOutputChannel
	 */
    void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel) throws Exception;


    /**
     * Detaches the duplex output channel and stops listening to response messages.
     * @throws Exception 
     */
    void detachDuplexOutputChannel();


    /**
     * Returns true if the reference to the duplex output channel is stored.
     * 
     * @return
     */
    boolean isDuplexOutputChannelAttached();


    /**
     * Returns attached duplex output channel.
     * 
     * @see IDuplexOutputChannel
     */
    IDuplexOutputChannel getAttachedDuplexOutputChannel();
}
