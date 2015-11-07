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
 * Messaging system delivering messages via websockets.
 * It creates the communication channels using WebSockets for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: ws://127.0.0.1:6080/MyService/. <br/>
 *
 */
public class WebSocketMessagingSystemFactory implements IMessagingSystemFactory
{
    private class WebSocketInputConnectorFactory implements IInputConnectorFactory
    {
        public WebSocketInputConnectorFactory(IProtocolFormatter protocolFormatter, IServerSecurityFactory serverSecurityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
                myServerSecurityFactory = serverSecurityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IInputConnector createInputConnector(String inputConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new WebSocketInputConnector(inputConnectorAddress, myProtocolFormatter, myServerSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IProtocolFormatter myProtocolFormatter;
        private IServerSecurityFactory myServerSecurityFactory;
    }
    
    private class WebSocketOutputConnectorFactory implements IOutputConnectorFactory
    {
        public WebSocketOutputConnectorFactory(IProtocolFormatter protocolFormatter, IClientSecurityFactory clientSecurityFactory,
                int pingFrequency)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
                myClientSecurityFactory = clientSecurityFactory;
                myPingingFrequency = pingFrequency;
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
                return new WebSocketOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter, myClientSecurityFactory, myPingingFrequency);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
     
        private IProtocolFormatter myProtocolFormatter;
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
        this(new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the websocket messaging factory.
     * @param protocolFormatter formatter used for low-level messages between output and input channels.
     */
    public WebSocketMessagingSystemFactory(IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPingFrequency = 300000;
            myProtocolFormatter = protocolFormatter;
            
            myInputChannelThreading = new SyncDispatching();
            myOutputChannelThreading = myInputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
 
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory aFactory = new WebSocketOutputConnectorFactory(myProtocolFormatter, myClientSecurityFactory, myPingFrequency);
            
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, myDispatcherAfterMessageDecoded, aFactory);
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
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory aFactory = new WebSocketOutputConnectorFactory(myProtocolFormatter, myClientSecurityFactory, myPingFrequency);
            
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, aFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            
            IInputConnectorFactory aFactory = new WebSocketInputConnectorFactory(myProtocolFormatter, myServerSecurityFactory);
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
    public WebSocketMessagingSystemFactory setServerSecurity(IServerSecurityFactory serverSecurityFactory)
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
    public WebSocketMessagingSystemFactory setClientSecurity(IClientSecurityFactory clientSecurityFactory)
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
    public WebSocketMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
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
    public WebSocketMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
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
    
    /**
     * Sets frequency to send the websocket ping message.
     * The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * @param milliseconds frequency in milliseconds
     * @return this WebSocketMessagingSystemFactory
     */
    public WebSocketMessagingSystemFactory setPingFrequency(int milliseconds)
    {
        myPingFrequency = milliseconds;
        return this;
    }
    
    /**
     * Returns the ping frequency in milliseconds.
     * The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * @return ping frequency in milliseconds.
     */
    public int getPingFrequency()
    {
        return myPingFrequency;
    }
    
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();

    private IProtocolFormatter myProtocolFormatter;
    private IThreadDispatcher myDispatcherAfterMessageDecoded = new NoDispatching().getDispatcher();
    
    private int myPingFrequency;
    
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}
