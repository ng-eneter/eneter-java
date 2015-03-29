/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

public interface IMultiTypedMessagesFactory
{
    IMultiTypedMessageSender createMultiTypedMessageSender();
    
    ISyncMultitypedMessageSender createSyncMultiTypedMessageSender();
    
    IMultiTypedMessageReceiver createMultiTypedMessageReceiver();
}
