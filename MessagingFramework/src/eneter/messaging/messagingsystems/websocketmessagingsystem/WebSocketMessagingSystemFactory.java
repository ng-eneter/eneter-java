/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.threading.dispatching.*;


/**
 * Implements the messaging system delivering messages via websockets.
 * It creates the communication channels using WebSockets for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: ws://127.0.0.1:6080/MyService/. <br/>
 *
 */
public class WebSocketMessagingSystemFactory implements IMessagingSystemFactory
{
    private class WebSocketInputConnectorFactory implements IInputConnectorFactory
    {
        public WebSocketInputConnectorFactory(IServerSecurityFactory serverSecurityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myServerSecurityFactory = serverSecurityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IInputConnector createInputConnector(String receiverAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new WebSocketInputConnector(receiverAddress, myServerSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IServerSecurityFactory myServerSecurityFactory;
    }
    
    private class WebSocketOutputConnectorFactory implements IOutputConnectorFactory
    {
        public WebSocketOutputConnectorFactory(int pingFrequency, IClientSecurityFactory clientSecurityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myPingingFrequency = pingFrequency;
                myClientSecurityFactory = clientSecurityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IOutputConnector createOutputConnector(
                String serviceConnectorAddress, String clientConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new WebSocketOutputConnector(serviceConnectorAddress, myPingingFrequency, myClientSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
     
        private IClientSecurityFactory myClientSecurityFactory;
        private int myPingingFrequency;
    }
    
    
    /**
     * Constructs the websocket messaging factory.
     * The ping frequency is set to default value 5 minutes.
     * The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * (e.g. Android phone can drop the connection if there is no activity several minutes.)
     */
    public WebSocketMessagingSystemFactory()
    {
        this(300000, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the websocket messaging factory.
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    public WebSocketMessagingSystemFactory(IProtocolFormatter<?> protocolFormatter)
    {
        this(300000, protocolFormatter);
    }
    
    /**
     * Constructs the websocket messaging factory.
     * It allows to set the ping frequency. The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * (e.g. Android phone can drop the connection if there is no activity several minutes.)
     * @param pingFrequency frequency of pinging in milliseconds.
     */
    public WebSocketMessagingSystemFactory(int pingFrequency)
    {
        this(pingFrequency, new EneterProtocolFormatter());
    }
    

    /**
     * Constructs the websocket messaging factory.
     * It allows to set the ping frequency. The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * (e.g. Android phone can drop the connection if there is no activity several minutes.) 
     * 
     * @param pingFrequency how often the client pings the server to keep the connection alive. If set to 0, the pinging will not start.
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    private WebSocketMessagingSystemFactory(int pingFrequency, IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPingFrequency = pingFrequency;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using WebSocket.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/ 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory anOutputConnectorFactory = new WebSocketOutputConnectorFactory(myPingFrequency, myClientSecurityFactory);
            
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, anOutputConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using WebSocket.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/ 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory anOutputConnectorFactory = new WebSocketOutputConnectorFactory(myPingFrequency, myClientSecurityFactory);
            
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, anOutputConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using WebSocket.
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     * 
     * @param channelId Identifies this duplex input channel. The channel id must be a valid URI address (e.g. ws://127.0.0.1:8090/MyService/) the input channel will listen to.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            
            IInputConnectorFactory aFactory = new WebSocketInputConnectorFactory(myServerSecurityFactory);
            IInputConnector anInputConnector = aFactory.createInputConnector(channelId);
            
            return new DefaultDuplexInputChannel(channelId, aDispatcher, anInputConnector, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /**
     * Sets the factory that will be used for creation of secured server socket.
     * @param serverSecurityFactory
     */
    public void setServerSecurity(IServerSecurityFactory serverSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServerSecurityFactory = serverSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets the factory that will be used for creation of secured client socket.
     * @param clientSecurityFactory
     */
    public void setClientSecurity(IClientSecurityFactory clientSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myClientSecurityFactory = clientSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public WebSocketMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
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
    
    public WebSocketMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
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
    
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();

    private IProtocolFormatter<?> myProtocolFormatter;
    private int myPingFrequency;
    
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}
