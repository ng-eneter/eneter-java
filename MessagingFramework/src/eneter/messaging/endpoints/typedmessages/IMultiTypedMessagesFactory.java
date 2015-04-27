/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Creates multi-typed senders and receivers.
 *
 */
public interface IMultiTypedMessagesFactory
{
    /**
     * Creates multityped message sender which can send request messages and receive response messages.
     * @return
     */
    IMultiTypedMessageSender createMultiTypedMessageSender();
    
    /**
     * Creates mulityped message sender which sends a request message and then waits for the response.
     * @return
     */
    ISyncMultitypedMessageSender createSyncMultiTypedMessageSender();
    
    /**
     * Creates multityped message receiver which can receive request messages and send response messages.
     * @return
     */
    IMultiTypedMessageReceiver createMultiTypedMessageReceiver();
}
