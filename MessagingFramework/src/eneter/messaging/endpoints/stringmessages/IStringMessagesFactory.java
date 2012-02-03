/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.stringmessages;

/**
 * The interface declares the factory to create string message senders and receivers.
 *
 */
public interface IStringMessagesFactory
{
    /**
     * Creates the string message sender.
     * @return
     */
    IStringMessageSender CreateStringMessageSender();
    
    /**
     * Creates the string message receiver.
     * @return
     */
    IStringMessageReceiver CreateStringMessageReceiver();
}
