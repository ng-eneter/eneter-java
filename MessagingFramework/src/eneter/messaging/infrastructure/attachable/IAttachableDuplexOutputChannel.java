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
 * The interface declares methods to attach/detach one IDuplexOutputChannel.
 * 
 * The duplex output channel is used in the request-response communication by a sender
 * to send request messages and receive response messages.
 * 
 * @author Martin Valach and Ondrej Uzovic
 *
 */
public interface IAttachableDuplexOutputChannel
{
	/**
	 * Attaches the duplex output channel and opens the connection for listening to response messages.
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
     * Notice, unlike version 1.0, the value 'true' does not mean the connection is open. If the duplex output
     * channel was successfuly attached but the connection was broken, the channel stays attached but the connection is not open.
     * To detect if the attached channel is listening to response messages, check the property {@link IDuplexOutputChannel.isConnected}.
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
