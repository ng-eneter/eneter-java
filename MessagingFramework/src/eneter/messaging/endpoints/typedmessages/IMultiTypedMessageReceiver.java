/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;

public interface IMultiTypedMessageReceiver extends IAttachableDuplexInputChannel
{
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    <T> void registerRequestMessageReceiver(EventHandler<TypedRequestReceivedEventArgs<T>> handler, Class<T> clazz);
    
    <T> void unregisterRequestMessageReceiver(Class<T> clazz);
    
    Class<?>[] getRegisteredRequestMessageTypes();
    
    <TResponseMessage> void SendResponseMessage(String responseReceiverId, TResponseMessage responseMessage, Class<TResponseMessage> clazz) throws Exception;
}
