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

/**
 * Implements the messaging system delivering messages via UDP.
 * It creates the communication channels using UDP for sending and receiving messages.
 * The channel id must be a valid UDP URI address. E.g.: udp://127.0.0.1:6080/. <br/>
 *
 */
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
    
    /**
     * Constructs the UDP messaging factory.
     */
    public UdpMessagingSystemFactory()
    {
        this(new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the UDP messaging factory. 
     * @param protocolFromatter formatter used for low-level messaging between output and input channels
     */
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
    

    /**
     * Creates the output channel sending messages to the specified input channel by using UDP.
     * The output channel can send messages only to the input channel and not to the duplex input channel.
     * 
     */
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

    /**
     * Creates the input channel receiving messages from output channel by using UDP.
     */
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

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using UDP.
     */
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

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using UDP.
     */
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

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using UDP.
     */
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
