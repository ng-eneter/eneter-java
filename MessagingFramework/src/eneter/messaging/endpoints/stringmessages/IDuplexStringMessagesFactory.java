/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * Creates sender and receiver for text messages.
 *
 */
public interface IDuplexStringMessagesFactory
{
    /**
     * Creates message sender.
     * @return string message sender
     */
    IDuplexStringMessageSender createDuplexStringMessageSender();
    
    /**
     * Creates message receiver.
     * @return string message receiver
     */
    IDuplexStringMessageReceiver createDuplexStringMessageReceiver();
}
