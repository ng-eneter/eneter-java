/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;


public interface IMultiTypedMessageSender extends IAttachableDuplexOutputChannel
{
    Event<DuplexChannelEventArgs> connectionOpened();
    
    Event<DuplexChannelEventArgs> connectionClosed();
    
    <T> void registerResponseMessageReceiver(EventHandler<TypedResponseReceivedEventArgs<T>> handler, Class<T> clazz) throws Exception;
    
    <T> void unregisterResponseMessageReceiver(Class<T> clazz);
    
    Class<?>[] getRegisteredResponseMessageTypes();
    
    <TRequestMessage> void sendRequestMessage(TRequestMessage message, Class<TRequestMessage> clazz) throws Exception;
}
