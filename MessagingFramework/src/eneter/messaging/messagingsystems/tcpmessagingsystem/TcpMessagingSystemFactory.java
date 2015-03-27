/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;


/**
 * Creates output and input channels which use TCP.
 * 
 * It creates the communication channels which use TCP for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: tcp://127.0.0.1:6080/.
 *
 */
public class TcpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class TcpInputConnectorFactory implements IInputConnectorFactory
    {
        public TcpInputConnectorFactory(IProtocolFormatter protocolFormatter, IServerSecurityFactory securityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
                mySecurityFactory = securityFactory;
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
                return new TcpInputConnector(inputConnectorAddress, myProtocolFormatter, mySecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IProtocolFormatter myProtocolFormatter;
        private IServerSecurityFactory mySecurityFactory;
    }
    
    private class TcpOutputConnectorFactory implements IOutputConnectorFactory
    {
        public TcpOutputConnectorFactory(IProtocolFormatter protocolFormatter, IClientSecurityFactory securityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
                mySecurityFactory = securityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IOutputConnector createOutputConnector(String inputConnectorAddress, String outputConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new TcpOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter, mySecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IProtocolFormatter myProtocolFormatter;
        private IClientSecurityFactory mySecurityFactory;
    }
    
    
    /**
     * Constructs the TCP messaging factory.
     */
    public TcpMessagingSystemFactory()
    {
        this(new EneterProtocolFormatter());
    }

    
    /**
     * Constructs the TCP messaging factory.
     * 
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    public TcpMessagingSystemFactory(IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
            
            myInputChannelThreading = new SyncDispatching();
            myOutputChannelThreading = myInputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using TCP.
     * 
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.<br/>
     * <br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.<br/>
     * <br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. tcp://127.0.0.1:8090/
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory anOutputConnectorFactory = new TcpOutputConnectorFactory(myProtocolFormatter, myClientSecurityFactory);
            
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, myDispatcherAfterMessageDecoded, anOutputConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using TCP.
     * 
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. tcp://127.0.0.1:8090/
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory anOutputConnectorFactory = new TcpOutputConnectorFactory(myProtocolFormatter, myClientSecurityFactory);
            
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, anOutputConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using TCP.
     * 
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     * 
     * @param channelId Identifies this duplex input channel. The channel id must be a valid Ip address (e.g. 127.0.0.1:8090) the input channel will listen to.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            
            IInputConnectorFactory aFactory = new TcpInputConnectorFactory(myProtocolFormatter, myServerSecurityFactory);
            IInputConnector anInputConnector = aFactory.createInputConnector(channelId);
            
            return new DefaultDuplexInputChannel(channelId, aDispatcher, myDispatcherAfterMessageDecoded, anInputConnector);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets the factory that will be used for creation of server sockets.
     * @param serverSecurityFactory
     */
    public TcpMessagingSystemFactory setServerSecurity(IServerSecurityFactory serverSecurityFactory)
    {
        myServerSecurityFactory = (serverSecurityFactory != null) ? serverSecurityFactory : new NoneSecurityServerFactory();
        return this;
    }
    
    /**
     * Gets the factory that is used for creation of server sockets.
     * @return
     */
    public IServerSecurityFactory getServerSecurity()
    {
        return myServerSecurityFactory;
    }
    
    /**
     * Sets the factory that will be used for creation of secured client socket.
     * @param clientSecurityFactory
     */
    public TcpMessagingSystemFactory setClientSecurity(IClientSecurityFactory clientSecurityFactory)
    {
        myClientSecurityFactory = (clientSecurityFactory != null) ? clientSecurityFactory : new NoneSecurityClientFactory();
        return this;
    }
    
    /**
     * Gets the factory that is used for creation of client sockets.
     * @return
     */
    public IClientSecurityFactory getClientSecurity()
    {
        return myClientSecurityFactory;
    }
    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return
     */
    public TcpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
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
    public TcpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
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
    
    
    private IProtocolFormatter myProtocolFormatter;
    private IThreadDispatcher myDispatcherAfterMessageDecoded = new NoDispatching().getDispatcher();
    
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}
