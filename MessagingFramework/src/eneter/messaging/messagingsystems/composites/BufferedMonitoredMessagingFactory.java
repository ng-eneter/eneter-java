/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit.BufferedMessagingFactory;
import eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit.MonitoredMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class BufferedMonitoredMessagingFactory implements IMessagingSystemFactory
{
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, new XmlStringSerializer(),
                10000, // max offline time
                1000, 2000);
    }
    
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging, ISerializer serializer)
    {
        this(underlyingMessaging, serializer,
                10000, // max offline time
                1000, 2000);
    }
    
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            ISerializer serializer,

            // Buffered Messaging
            long maxOfflineTime,

            // Monitored Messaging
            long pingFrequency,
            long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IMessagingSystemFactory aMonitoredMessaging = new MonitoredMessagingFactory(underlyingMessaging, serializer, pingFrequency, pingResponseTimeout);
            myBufferedMessaging = new BufferedMessagingFactory(aMonitoredMessaging, maxOfflineTime);
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
            return myBufferedMessaging.createOutputChannel(channelId);
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
            return myBufferedMessaging.createInputChannel(channelId);
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
            return myBufferedMessaging.createDuplexOutputChannel(channelId);
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
            return myBufferedMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
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
            return myBufferedMessaging.createDuplexInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myBufferedMessaging;
}
