/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import eneter.messaging.dataprocessing.messagequeueing.internal.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;

/**
 * Implements the messaging system delivering messages via TCP.
 * 
 * It creates the communication channels using TCP for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: tcp://127.0.0.1:6080/.
 *
 */
public class TcpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class TcpServiceConnectorFactory implements IInputConnectorFactory
    {
        public TcpServiceConnectorFactory(IServerSecurityFactory securityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                mySecurityFactory = securityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        @Override
        public IInputConnector createInputConnector(
                String receiverAddress) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new TcpServiceConnector(receiverAddress, mySecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IServerSecurityFactory mySecurityFactory;
    }
    
    private class TcpClientConnectorFactory implements IOutputConnectorFactory
    {
        public TcpClientConnectorFactory(IClientSecurityFactory securityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                mySecurityFactory = securityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IOutputConnector createClientConnector(
                String serviceConnectorAddress, String clientConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new TcpClientConnector(serviceConnectorAddress, mySecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private IClientSecurityFactory mySecurityFactory;
    }
    
    
    /**
     * Constructs the TCP messaging factory.
     */
    public TcpMessagingSystemFactory()
    {
        this(EConcurrencyMode.Synchronous, new EneterProtocolFormatter());
    }

    /**
     * Constructs the TCP messaging factory.
     * @param concurrencyMode Specifies the threading mode for receiving messages in input channel and duplex input channel.
     */
    public TcpMessagingSystemFactory(EConcurrencyMode concurrencyMode)
    {
        this(concurrencyMode, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the TCP messaging factory.
     * 
     * @param concurrencyMode Specifies the threading mode for receiving messages in input channel and duplex input channel.
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    public TcpMessagingSystemFactory(EConcurrencyMode concurrencyMode, IProtocolFormatter<byte[]> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConcurrencyMode = concurrencyMode;
            myProtocolFormatter = protocolFormatter;
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            IOutputConnectorFactory aClientConnectorFactory = new TcpClientConnectorFactory(myClientSecurityFactory);
            return new DefaultDuplexOutputChannel(channelId, null, anInvoker, myProtocolFormatter, aClientConnectorFactory, false);
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
            IInvoker anInvoker = new WorkingThreadInvoker();
            IOutputConnectorFactory aClientConnectorFactory = new TcpClientConnectorFactory(myClientSecurityFactory);
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, anInvoker, myProtocolFormatter, aClientConnectorFactory, false);
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
            IInvoker anInvoker = null;
            if (myConcurrencyMode == EConcurrencyMode.Synchronous)
            {
                anInvoker = new WorkingThreadInvoker(channelId);
            }
            else
            {
                anInvoker = new CallingThreadInvoker();
            }
            
            IInputConnectorFactory aFactory = new TcpServiceConnectorFactory(myServerSecurityFactory);
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
    
    
    private EConcurrencyMode myConcurrencyMode;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();
}
