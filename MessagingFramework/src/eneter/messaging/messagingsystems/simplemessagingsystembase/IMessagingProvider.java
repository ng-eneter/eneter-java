/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */
package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

public interface IMessagingProvider
{
    void sendMessage(String receiverId, Object message)
            throws Exception;
    
    void registerMessageHandler(String receiverId, IMethod1<Object> messageHandler);

    void unregisterMessageHandler(String receiverId);
}
