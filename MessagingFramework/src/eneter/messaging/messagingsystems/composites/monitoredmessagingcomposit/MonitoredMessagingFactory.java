/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class MonitoredMessagingFactory implements IMessagingSystemFactory
{
    
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, new XmlStringSerializer(), 1000, 2000);
    }
    
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            ISerializer serializer,
            long pingFrequency,
            long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessaging;
            mySerializer = serializer;
            myPingFrequency = pingFrequency;
            myPingResponseTimeout = pingResponseTimeout;
            myResponseReceiverTimeout = pingFrequency + pingResponseTimeout;
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
            return myUnderlyingMessaging.createOutputChannel(channelId);
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
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myPingResponseTimeout);
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
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myPingResponseTimeout);
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
            IDuplexInputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new MonitoredDuplexInputChannel(anUnderlyingChannel, mySerializer, myResponseReceiverTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myUnderlyingMessaging;
    private long myPingFrequency;
    private long myPingResponseTimeout;
    private long myResponseReceiverTimeout;
    private ISerializer mySerializer;
}
