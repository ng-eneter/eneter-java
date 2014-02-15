/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * The interface declares the factory to create message sender and receiver for text messages.
 *
 */
public interface IDuplexStringMessagesFactory
{
    /**
     * Creates message sender.
     * @return message sender
     */
    IDuplexStringMessageSender createDuplexStringMessageSender();
    
    /**
     * Creates message receiver.
     * @return message receiver
     */
    IDuplexStringMessageReceiver createDuplexStringMessageReceiver();
}
