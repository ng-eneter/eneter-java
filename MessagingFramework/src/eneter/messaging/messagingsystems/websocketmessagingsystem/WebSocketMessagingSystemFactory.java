/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import eneter.messaging.dataprocessing.messagequeueing.internal.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;


/**
 * Implements the messaging system delivering messages via websockets.
 * It creates the communication channels using WebSockets for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: ws://127.0.0.1:6080/MyService/. <br/>
 *
 */
public class WebSocketMessagingSystemFactory implements IMessagingSystemFactory
{
    private class WebSocketServerConnectorFactory implements IServiceConnectorFactory
    {
        public WebSocketServerConnectorFactory(IServerSecurityFactory serverSecurityFactory)
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
        public IServiceConnector createServiceConnector(String receiverAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new WebSocketServiceConnector(receiverAddress, myServerSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IServerSecurityFactory myServerSecurityFactory;
    }
    
    private class WebSocketClientConnectorFactory implements IClientConnectorFactory
    {
        public WebSocketClientConnectorFactory(int pingFrequency, IClientSecurityFactory clientSecurityFactory)
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
        public IClientConnector createClientConnector(
                String serviceConnectorAddress, String clientConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new WebSocketClientConnector(serviceConnectorAddress, myPingingFrequency, myClientSecurityFactory);
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
        this(EConcurrencyMode.Synchronous, 300000, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the websocket messaging factory.
     * @param concurrencyMode Specifies the threading mode for receiving messages in input channel and duplex input channel.
     */
    public WebSocketMessagingSystemFactory(EConcurrencyMode concurrencyMode)
    {
        this(concurrencyMode, 300000, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the websocket messaging factory.
     * @param concurrencyMode Specifies the threading mode for receiving messages in input channel and duplex input channel.
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    public WebSocketMessagingSystemFactory(EConcurrencyMode concurrencyMode, IProtocolFormatter<?> protocolFormatter)
    {
        this(concurrencyMode, 300000, protocolFormatter);
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
        this(EConcurrencyMode.Synchronous, pingFrequency, new EneterProtocolFormatter());
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
    public WebSocketMessagingSystemFactory(int pingFrequency, IProtocolFormatter<?> protocolFormatter)
    {
        this(EConcurrencyMode.Synchronous, pingFrequency, new EneterProtocolFormatter());
    }

    /**
     * Constructs the websocket messaging factory.
     * It allows to set the ping frequency. The pinging is intended to keep the connection alive in
     * environments that would drop the connection if not active for some time.
     * (e.g. Android phone can drop the connection if there is no activity several minutes.) 
     * 
     * @param concurrencyMode Specifies the threading mode for receiving messages in input channel and duplex input channel.
     * @param pingFrequency how often the client pings the server to keep the connection alive. If set to 0, the pinging will not start.
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    private WebSocketMessagingSystemFactory(EConcurrencyMode concurrencyMode, int pingFrequency, IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConcurrencyMode = concurrencyMode;
            myPingFrequency = pingFrequency;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the output channel sending messages to the specified input channel by using WebSocket.
     * The output channel can send messages only to the input channel and not to the duplex input channel.
     * 
     * @param channelId Identifies the receiving output channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/
     */
    @Override
    public IOutputChannel createOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IClientConnectorFactory aFactory = new WebSocketClientConnectorFactory(myPingFrequency, myClientSecurityFactory);
            return new DefaultOutputChannel(channelId, myProtocolFormatter, aFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the input channel receiving messages from output channel by using WebSocket.
     * @param channelId The addres, the input channel will listen to. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/.
     */
    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IInvoker anInvoker = null;
            if (myConcurrencyMode == EConcurrencyMode.Synchronous)
            {
                anInvoker = new WorkingThreadInvoker(channelId);
            }
            else
            {
                anInvoker = new CallingThreadInvoker();
            }
            
            IServiceConnectorFactory aFactory = new WebSocketServerConnectorFactory(myServerSecurityFactory);
            return new DefaultInputChannel(channelId, anInvoker, myProtocolFormatter, aFactory);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            IClientConnectorFactory aFactory = new WebSocketClientConnectorFactory(myPingFrequency, myClientSecurityFactory);
            return new DefaultDuplexOutputChannel(channelId, null, anInvoker, myProtocolFormatter, aFactory, false);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            IClientConnectorFactory aFactory = new WebSocketClientConnectorFactory(myPingFrequency, myClientSecurityFactory);
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, anInvoker, myProtocolFormatter, aFactory, false);
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
            IInvoker anInvoker = null;
            if (myConcurrencyMode == EConcurrencyMode.Synchronous)
            {
                anInvoker = new WorkingThreadInvoker(channelId);
            }
            else
            {
                anInvoker = new CallingThreadInvoker();
            }
            
            IServiceConnectorFactory aFactory = new WebSocketServerConnectorFactory(myServerSecurityFactory);
            return new DefaultDuplexInputChannel(channelId, anInvoker, myProtocolFormatter, aFactory);
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
    
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();

    private EConcurrencyMode myConcurrencyMode;
    private IProtocolFormatter<?> myProtocolFormatter;
    private int myPingFrequency;
}
