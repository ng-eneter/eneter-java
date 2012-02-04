/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IMethod1;

public class SimpleMessagingSystem implements IMessagingSystemBase
{
    public SimpleMessagingSystem(IMessagingProvider inputChannelMessaging)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelMessaging = inputChannelMessaging;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void sendMessage(String channelId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelMessaging.sendMessage(channelId, message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void registerMessageHandler(String channelId, IMethod1<Object> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelMessaging.registerMessageHandler(channelId, messageHandler);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void unregisterMessageHandler(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelMessaging.unregisterMessageHandler(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private IMessagingProvider myInputChannelMessaging;
}
