/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;


public interface ISyncMultitypedMessageSender
{
    Event<DuplexChannelEventArgs> connectionOpened();
    
    Event<DuplexChannelEventArgs> connectionClosed();
    
    <TRequest, TResponse> TResponse sendRequestMessage(TRequest message, Class<TRequest> requestClazz, Class<TResponse> responseClazz);
}
