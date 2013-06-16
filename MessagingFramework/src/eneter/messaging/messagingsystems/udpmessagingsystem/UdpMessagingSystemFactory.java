/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import eneter.messaging.dataprocessing.messagequeueing.internal.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;


public class UdpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class UdpConnectorFactory implements IClientConnectorFactory, IServiceConnectorFactory
    {
        @Override
        public IClientConnector createClientConnector(
                String serviceConnectorAddress, String clientConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpClientConnector(serviceConnectorAddress);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IServiceConnector createServiceConnector(
                String serviceConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpServiceConnector(serviceConnectorAddress);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
    }
    
    public UdpMessagingSystemFactory()
    {
        this(new EneterProtocolFormatter());
    }
    
    public UdpMessagingSystemFactory(IProtocolFormatter<?> protocolFromatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFromatter;
            myConnectorFactory = new UdpConnectorFactory();
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
            return new DefaultOutputChannel(channelId, myProtocolFormatter, myConnectorFactory);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            return new DefaultInputChannel(channelId, anInvoker, myProtocolFormatter, myConnectorFactory);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            return new DefaultDuplexOutputChannel(channelId, null, anInvoker, myProtocolFormatter, myConnectorFactory, false);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, anInvoker, myProtocolFormatter, myConnectorFactory, false);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            return new DefaultDuplexInputChannel(channelId, anInvoker, myProtocolFormatter, myConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IProtocolFormatter<?> myProtocolFormatter;
    private UdpConnectorFactory myConnectorFactory;
}
