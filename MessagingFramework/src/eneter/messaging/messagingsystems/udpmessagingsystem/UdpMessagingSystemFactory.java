/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
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
        public UdpConnectorFactory(IProtocolFormatter protocolFormatter, boolean reuseAddressFlag, int responseReceivingPort,
                boolean isUnicast,
                boolean allowReceivingBroadcasts, int ttl, String multicastGroup, boolean multicastLoopbackFlag)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
                myReuseAddressFlag = reuseAddressFlag;
                myResponseReceivingPort = responseReceivingPort;

                myIsUnicastFlag = isUnicast;

                myAllowReceivingBroadcasts = allowReceivingBroadcasts;
                myTtl = ttl;
                myMulticastGroup = multicastGroup;
                myMulticastLoopbackFlag = multicastLoopbackFlag;
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
                IOutputConnector anOutputConnector;
                if (!myIsUnicastFlag)
                {
                    anOutputConnector = new UdpSessionlessOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter, myReuseAddressFlag, myTtl, myAllowReceivingBroadcasts, myMulticastGroup, myMulticastLoopbackFlag);
                }
                else
                {
                    anOutputConnector = new UdpOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter, myReuseAddressFlag, myResponseReceivingPort, myTtl);
                }

                return anOutputConnector;
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
                IInputConnector anInputConnector;
                if (!myIsUnicastFlag)
                {
                    anInputConnector = new UdpSessionlessInputConnector(inputConnectorAddress, myProtocolFormatter, myReuseAddressFlag, myTtl, myAllowReceivingBroadcasts, myMulticastGroup, myMulticastLoopbackFlag);
                }
                else
                {
                    anInputConnector = new UdpInputConnector(inputConnectorAddress, myProtocolFormatter, myReuseAddressFlag, myTtl);
                }

                return anInputConnector;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IProtocolFormatter myProtocolFormatter;
        private boolean myReuseAddressFlag;
        private int myResponseReceivingPort;
        private boolean myIsUnicastFlag;
        private boolean myAllowReceivingBroadcasts;
        private int myTtl;
        private String myMulticastGroup;
        private boolean myMulticastLoopbackFlag;
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
            myProtocolFormatter = protocolFromatter;
            myInputChannelThreading = new SyncDispatching();
            myOutputChannelThreading = myInputChannelThreading;
            myTtl = 128;
            myResponseReceiverPort = -1;
            myUnicastCommunication = true;
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
            String aResponseReceiverId = null;

            if (!myUnicastCommunication)
            {
                aResponseReceiverId = "udp://0.0.0.0/";
            }
            
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory aConnectorFactory = new UdpConnectorFactory(myProtocolFormatter, myReuseAddress, myResponseReceiverPort, myUnicastCommunication, myAllowSendingBroadcasts, myTtl, myMulticastGroupToReceive, myMulticastLoopback);
            return new DefaultDuplexOutputChannel(channelId, aResponseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, aConnectorFactory);
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
            IOutputConnectorFactory aConnectorFactory = new UdpConnectorFactory(myProtocolFormatter, myReuseAddress, myResponseReceiverPort, myUnicastCommunication, myAllowSendingBroadcasts, myTtl, myMulticastGroupToReceive, myMulticastLoopback);
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, aConnectorFactory);
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
            
            IInputConnectorFactory aConnectorFactory = new UdpConnectorFactory(myProtocolFormatter, myReuseAddress, -1, myUnicastCommunication, myAllowSendingBroadcasts, myTtl, myMulticastGroupToReceive, myMulticastLoopback);
            IInputConnector anInputConnector = aConnectorFactory.createInputConnector(channelId);
 
            return new DefaultDuplexInputChannel(channelId, aDispatcher, myDispatcherAfterMessageDecoded, anInputConnector);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public UdpMessagingSystemFactory setUnicastCommunication(boolean isUnicast)
    {
        myUnicastCommunication = isUnicast;
        return this;
    }
    
    public boolean getUnicastCommunication()
    {
        return myUnicastCommunication;
    }
    
    public UdpMessagingSystemFactory setTtl(int ttl)
    {
        myTtl = ttl;
        return this;
    }
    
    public int getTtl()
    {
        return myTtl;
    }
    
    
    public UdpMessagingSystemFactory setMulticastGroupToReceive(String multicastGroup)
    {
        myMulticastGroupToReceive = multicastGroup;
        return this;
    }
    
    public String getMulticastGroupToReceive()
    {
        return myMulticastGroupToReceive;
    }
    
    public UdpMessagingSystemFactory setAllowSendingBroadcasts(boolean allowBroadcasts)
    {
        myAllowSendingBroadcasts = allowBroadcasts;
        return this;
    }
    
    public boolean getAllowSendingBroadcasts()
    {
        return myAllowSendingBroadcasts;
    }
    
    public UdpMessagingSystemFactory setMulticastLoopback(boolean allowMulticastLoopback)
    {
        myMulticastLoopback = allowMulticastLoopback;
        return this;
    }
    
    public boolean getMulticastLoopback()
    {
        return myMulticastLoopback;
    }
    
    public UdpMessagingSystemFactory setResponseReceiverPort(int port)
    {
        myResponseReceiverPort = port;
        return this;
    }
    
    public int getResponseReceiverPort()
    {
        return myResponseReceiverPort;
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
    
    
    
    public boolean getReuseAddress()
    {
        return myReuseAddress;
    }
    
    public UdpMessagingSystemFactory setReuseAddress(boolean allowReuseAddressFlag)
    {
        myReuseAddress = allowReuseAddressFlag;
        return this;
    }
    
    
    private IProtocolFormatter myProtocolFormatter;
    private IThreadDispatcher myDispatcherAfterMessageDecoded = new NoDispatching().getDispatcher();
    
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
    private boolean myReuseAddress;
    private int myResponseReceiverPort;
    private boolean myMulticastLoopback;
    private boolean myAllowSendingBroadcasts;
    private String myMulticastGroupToReceive;
    private int myTtl;
    private boolean myUnicastCommunication;
}
