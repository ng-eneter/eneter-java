/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

public class SimpleMessagingSystem implements IMessagingSystemBase
{
    public SimpleMessagingSystem(IMessagingProvider inputChannelMessaging)
    {
        myInputChannelMessaging = inputChannelMessaging;
    }

    public void sendMessage(String channelId, Object message)
            throws Exception
    {
        myInputChannelMessaging.sendMessage(channelId, message);
    }

    public void registerMessageHandler(String channelId, IMethod1<Object> messageHandler)
    {
        myInputChannelMessaging.registerMessageHandler(channelId, messageHandler);
    }

    public void unregisterMessageHandler(String channelId)
    {
        myInputChannelMessaging.unregisterMessageHandler(channelId);
    }
    

    private IMessagingProvider myInputChannelMessaging;
}
