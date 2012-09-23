/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.net.system.IMethod1;

public interface IMessagingSystemBase
{
    void sendMessage(String channelId, Object message)
            throws Exception;
    
    void registerMessageHandler(String channelId, IMethod1<Object> messageHandler);
    
    void unregisterMessageHandler(String channelId);
}
