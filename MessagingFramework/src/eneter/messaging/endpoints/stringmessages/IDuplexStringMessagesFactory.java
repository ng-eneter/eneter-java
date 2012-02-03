/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * The interface declares the factory to create duplex string message sender and receiver.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IDuplexStringMessagesFactory
{
    /**
     * Creates the duplex string message sender.
     * @return duplex string message sender
     */
    IDuplexStringMessageSender createDuplexStringMessageSender();
    
    /**
     * Creates the duplex string message receiver.
     * @return duplex string message receiver
     */
    IDuplexStringMessageReceiver createDuplexStringMessageReceiver();
}
