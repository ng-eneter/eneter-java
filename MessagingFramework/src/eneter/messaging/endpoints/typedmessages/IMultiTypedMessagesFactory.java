/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Creates multi-typed message senders and receivers.
 *
 */
public interface IMultiTypedMessagesFactory
{
    /**
     * Creates multityped message sender which can send request messages and receive response messages.
     * @return multityped sender
     */
    IMultiTypedMessageSender createMultiTypedMessageSender();
    
    /**
     * Creates mulityped message sender which sends a request message and then waits for the response.
     * @return synchronous multityped sender
     */
    ISyncMultitypedMessageSender createSyncMultiTypedMessageSender();
    
    /**
     * Creates multityped message receiver which can receive request messages and send response messages.
     * @return multityped receiver
     */
    IMultiTypedMessageReceiver createMultiTypedMessageReceiver();
}
