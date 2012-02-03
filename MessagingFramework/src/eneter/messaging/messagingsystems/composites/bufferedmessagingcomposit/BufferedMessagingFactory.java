/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class BufferedMessagingFactory implements IMessagingSystemFactory
{
    public BufferedMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, 10000);
    }
    
    public BufferedMessagingFactory(IMessagingSystemFactory underlyingMessaging, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessaging;
            myMaxOfflineTime = maxOfflineTime;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IOutputChannel createOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IOutputChannel anUnderlyingOutputChannel = myUnderlyingMessaging.createOutputChannel(channelId);
            return new BufferedOutputChannel(anUnderlyingOutputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myUnderlyingMessaging.createInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingDuplexOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new BufferedDuplexOutputChannel(anUnderlyingDuplexOutputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingDuplexOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new BufferedDuplexOutputChannel(anUnderlyingDuplexOutputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexInputChannel anUnderlyingDuplexInputChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new BufferedDuplexInputChannel(anUnderlyingDuplexInputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private IMessagingSystemFactory myUnderlyingMessaging;
    private long myMaxOfflineTime;
}
