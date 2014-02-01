/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;


/**
 * Implements the messaging system delivering messages via UDP.
 * It creates the communication channels using UDP for sending and receiving messages.
 * The channel id must be a valid UDP URI address. E.g.: udp://127.0.0.1:6080/. <br/>
 *
 */
public class UdpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class UdpConnectorFactory implements IOutputConnectorFactory, IInputConnectorFactory
    {
        @Override
        public IOutputConnector createOutputConnector(
                String serviceConnectorAddress, String clientConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpOutputConnector(serviceConnectorAddress);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IInputConnector createInputConnector(
                String serviceConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpInputConnector(serviceConnectorAddress);
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
            
            myInputChannelThreading = new SyncDispatching();
            myOutputChannelThreading = myInputChannelThreading;
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
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, myConnectorFactory, myProtocolFormatter, false);
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
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myConnectorFactory, myProtocolFormatter, false);
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
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            IInputConnector anInputConnector = myConnectorFactory.createInputConnector(channelId);
            return new DefaultDuplexInputChannel(channelId, aDispatcher, anInputConnector, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IProtocolFormatter<?> getProtocolFormatter()
    {
        return myProtocolFormatter;
    }
    
    public UdpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelThreading = inputChannelThreading;
            return this;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myInputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public UdpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputChannelThreading = outputChannelThreading;
            return this;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myOutputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IProtocolFormatter<?> myProtocolFormatter;
    private UdpConnectorFactory myConnectorFactory;
    
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}
