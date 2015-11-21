/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;


/**
 * Messaging system delivering messages via TCP.
 * 
 * It creates the communication channels which use TCP for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: tcp://127.0.0.1:6080/.<br/>
 * <br/>
 * Creating input channel for TCP messaging.
 * <pre>
 * <code>
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * 
 * // Create duplex input channel which can receive messages on the address 127.0.0.1 and the port 9043
 * // and which can send response messages to connected output channels.
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:9043/");
 * 
 * // Subscribe to handle messages.
 * anInputChannel.messageReceived().subscribe(myOnMessageReceived);
 * 
 * // Start listening and be able to receive messages.
 * anInputChannel.startListening();
 * 
 * ...
 * 
 * // Stop listening.
 * anInputChannel.stopListeing();
 * </code>
 * </pre>
 * Creating output channel for TCP messaging.
 * <pre>
 * <code>
 * // Create duplex output channel which can send messages to 127.0.0.1 on the port 9043 and
 * // receive response messages.
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:9043/");
 * 
 * // Subscribe to handle messages.
 * anOutputChannel.responseMessageReceived().subscribe(myOnMessageReceived);
 * 
 * // Open connection to the input channel which listens to tcp://127.0.0.1:9043/.
 * anOutputChannel.openConnection();
 * 
 * ...
 * 
 * // Close connection.
 * anOutputChannel.closeConnection();
 * </code>
 * </pre>
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
     * @param protocolFormatter formats OpenConnection, CloseConnection and Message messages between channels.
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
     * Creates duplex output channel which can send and receive messages from the duplex input channel using TCP.
     * 
     * Creating the duplex output channel.<br/>
     * <br/>
     * <pre>
     * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8765/");
     * </pre>
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
     * Creates duplex output channel which can send and receive messages from the duplex input channel using TCP.
     * <br/>
     * Creating the duplex output channel with specified client id.
     * <pre>
     * <code>
     * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8765/", "MyUniqueClientId_1");
     * </code>
     * </pre>
     * 
     * @param channelId Identifies the input channel which shall be connected. The channel id must be a valid URI address e.g. tcp://127.0.0.1:8090/
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
     * Creates the duplex input channel which can receive and send messages to the duplex output channel using TCP.
     * <br/>
     * Creating duplex input channel.
     * <pre>
     * <code>
     * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
     * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:9876/");
     * </code>
     * </pre>
     * 
     * @param channelId The IP address and port which shall be used for listening.
     *  The channel id must be a valid URI address (e.g. tcp://127.0.0.1:8090/).<br/>
     *  If the IP address is 0.0.0.0 then it will listen to all available IP addresses.
     *  E.g. if the address is tcp://0.0.0.0:8033/ then it will listen to all available IP addresses on the port 8033.
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
     * Returns IP addresses assigned to the device which can be used for the listening.
     * 
     * @return IP addresses which can be used for the listening.
     * @throws SocketException
     */
    public static String[] getAvailableIpAddresses() throws SocketException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> anIpAddresses = new ArrayList<String>();
            for (Enumeration<NetworkInterface> i = NetworkInterface.getNetworkInterfaces(); i.hasMoreElements();)
            {
                NetworkInterface aNetworkInterface = i.nextElement();
                if (aNetworkInterface != null)
                {
                    for (Enumeration<InetAddress> j = aNetworkInterface.getInetAddresses(); j.hasMoreElements();)
                    {
                        String anAddress = j.nextElement().getHostAddress();
                        anIpAddresses.add(anAddress);
                    }
                }
            }
            
            String[] aResult = new String[anIpAddresses.size()];
            return anIpAddresses.toArray(aResult);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /**
     * Sets the factory that will be used for creation of server sockets.
     * 
     * Except security (e.g. using encrypted SSL communication) the factory also allows to specify other communication
     * parameters e.g. timeouts, buffers, etc.  
     * 
     * @param serverSecurityFactory
     */
    public TcpMessagingSystemFactory setServerSecurity(IServerSecurityFactory serverSecurityFactory)
    {
        myServerSecurityFactory = (serverSecurityFactory != null) ? serverSecurityFactory : new NoneSecurityServerFactory();
        return this;
    }
    
    /**
     * Gets the factory that is used for creation of server sockets.
     * 
     * Except security (e.g. using encrypted SSL communication) the factory also allows to specify other communication
     * parameters e.g. timeouts, buffers, etc.
     * 
     * @return
     */
    public IServerSecurityFactory getServerSecurity()
    {
        return myServerSecurityFactory;
    }
    
    /**
     * Sets the factory that will be used for creation of secured client socket.
     * 
     * Except security (e.g. using encrypted SSL communication) the factory also allows to specify other communication
     * parameters e.g. timeouts, buffers, etc.
     * 
     * @param clientSecurityFactory
     */
    public TcpMessagingSystemFactory setClientSecurity(IClientSecurityFactory clientSecurityFactory)
    {
        myClientSecurityFactory = (clientSecurityFactory != null) ? clientSecurityFactory : new NoneSecurityClientFactory();
        return this;
    }
    
    /**
     * Gets the factory that is used for creation of client sockets.
     * 
     * Except security (e.g. using encrypted SSL communication) the factory also allows to specify other communication
     * parameters e.g. timeouts, buffers, etc.
     * 
     * @return client socket factory
     */
    public IClientSecurityFactory getClientSecurity()
    {
        return myClientSecurityFactory;
    }
    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return this TcpMessagingSystemFactory
     */
    public TcpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return thread dispatcher 
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return this TcpMessagingSystemFactory
     */
    public TcpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return thread dispatcher
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
