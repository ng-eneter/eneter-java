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
 * Messaging system delivering messages via UDP.
 * It creates the communication channels using UDP for sending and receiving messages.
 * The channel id must be a valid UDP URI address. E.g.: udp://127.0.0.1:6080/. <br/>
 *
 */
public class UdpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class UdpConnectorFactory implements IOutputConnectorFactory, IInputConnectorFactory
    {
        public UdpConnectorFactory(IProtocolFormatter protocolFormatter)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        @Override
        public IOutputConnector createOutputConnector(String inputConnectorAddress, String outputConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IInputConnector createInputConnector(String inputConnectorAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new UdpInputConnector(inputConnectorAddress, myProtocolFormatter);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IProtocolFormatter myProtocolFormatter;
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
    public UdpMessagingSystemFactory(IProtocolFormatter protocolFromatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectorFactory = new UdpConnectorFactory(protocolFromatter);
            
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
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, myDispatcherAfterMessageDecoded, myConnectorFactory);
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
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, myConnectorFactory);
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
            return new DefaultDuplexInputChannel(channelId, aDispatcher, myDispatcherAfterMessageDecoded, anInputConnector);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return
     */
    public UdpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return
     */
    public UdpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    
    private UdpConnectorFactory myConnectorFactory;
    private IThreadDispatcher myDispatcherAfterMessageDecoded = new NoDispatching().getDispatcher();
    
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}
