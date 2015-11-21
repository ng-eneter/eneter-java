/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;
import eneter.net.system.EventHandler;


/**
 * Messaging system delivering messages via UDP.
 * 
 * It creates the communication channels using UDP for sending and receiving messages.
 * The channel id must be a valid UDP URI address. E.g.: udp://127.0.0.1:6080/. <br/>
 * The messaging via UDP supports unicast, multicast and broadcast communication.<br/>
 * The unicast communication is the routing of messages from one sender to one receiver.
 * (E.g. a client-service communication where a client sends messages to one service and the service 
 * can send response messages to one client.)<br/> 
 * The multicast communication is the routing of messages from one sender to multiple receivers
 * (the receivers which joined the specific multicast group and listen to the specific port).
 * The broadcast communication is the routing of messages from one sender to all receivers within the sub-net which listen
 * to the specific port.<br/>
 * <br/>
 * UDP unicast communication.<br/>
 * Unicast input channel:
 * <pre>
 * <code>
 * // Create UDP input channel.
 * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8043/");
 * 
 * // Subscribe for receiving messages.
 * anInputChannel.messageReceived().subscribe(myOnMessageReceived);
 * 
 * // Start listening.
 * anInputChannel.startListening();
 * 
 * ...
 * 
 * // Stop listening.
 * anInputChannel.stopListening();
 * 
 * 
 * // Handling of messages.
 * private void onMessageReceived(object sender, DuplexChannelMessageEventArgs e)
 * {
 *     // Handle incoming message.
 *     ....
 *     
 *     // Send response message.
 *     IDuplexInputChannel anInputChannel = (IDuplexInputChannel)sender;
 *     anInputChannel.sendResponseMessage(e.ResponseReceiverId, "Hi");
 * }
 * private EventHandler&lt;DuplexChannelMessageEventArgs&gt; myOnMessageReceived = new EventHandler&lt;DuplexChannelMessageEventArgs&gt;()
 * {
 *     {@literal @}Override
 *     public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
 *     {
 *         onMessageReceived(sender, e);
 *     }
 * };
 * </code>
 * </pre>
 * Unicast output channel:
 * <pre>
 * <code>
 * // Create UDP output channel.
 * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8043/");
 * 
 * // Subscribe to receive messages.
 * anOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
 * 
 * // Open the connection.
 * anOutputChannel.openConnection();
 * 
 * ...
 * 
 * // Send a message.
 * anOutputChannel.sendMessage("Hello");
 * 
 * ...
 * // Close connection.
 * anOutputChannel.closeConnection();
 * 
 * 
 * // Handling of received message.
 * private void onResponseMessageReceived(object sender, DuplexChannelMessageEventArgs e)
 * {
 *     string aMessage = (string)e.Message;
 *     ....
 * }
 * 
 * private EventHandler&lt;DuplexChannelMessageEventArgs&gt; myOnResponseMessageReceived = new EventHandler&ltDuplexChannelMessageEventArgs&gt;()
 * {
 *     {@literal @}Override
 *     public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
 *     {
 *         onResponseMessageReceived(sender, e);
 *     }
 * };
 * </code>
 * </pre>
 * <br/>
 * <br/>
 * UDP multicast communication.<br/>
 * Multicast input channel:
 * <pre>
 * <code>
 * // Create UDP input channel.
 * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory()
 *    // The communication will be multicast or broadcast.
 *    .setUnicastCommunication(false)
 *    // The multicast group which shall be joined.
 *    .setMulticastGroupToReceive("234.5.6.7");
 * 
 * // This input channel will be able to receive messages sent to udp://127.0.0.1:8043/ or
 * // to the multicast group udp://234.5.6.7:8043/.
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8043/")
 * 
 * 
 * // Subscribe for receiving messages.
 * anInputChannel.messageReceived().subscribe(myOnMessageReceived);
 * 
 * // Start listening.
 * anInputChannel.startListening();
 * 
 * ...
 * 
 * // Stop listening.
 * anInputChannel.stopListening();
 * 
 * 
 * // Handling of messages.
 * private void onMessageReceived(object sender, DuplexChannelMessageEventArgs e)
 * {
 *     // Handle incoming message.
 *     ....
 *     
 *     // Send response message.
 *     IDuplexInputChannel anInputChannel = (IDuplexInputChannel)sender;
 *     anInputChannel.sendResponseMessage(e.ResponseReceiverId, "Hi");
 * }
 * 
 * private EventHandler&lt;DuplexChannelMessageEventArgs&gt; myOnMessageReceived = new EventHandler&lt;DuplexChannelMessageEventArgs&gt;()
 * {
 *     {@literal @}Override
 *     public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
 *     {
 *         onMessageReceived(sender, e);
 *     }
 * };
 * </code>
 * </pre>
 * Output channel sending messages to the multicast group.
 * <pre>
 * <code>
 * // Create UDP output channel which will send messages to the multicast group udp://234.5.6.7:8043/.
 * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory()
 *    // The communication will be multicast or broadcast.
 *    .setUnicastCommunication(false);
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://234.5.6.7:8043/");
 * 
 * // Subscribe to receive messages.
 * anOutputChannel.responseMessageReceived(myOnResponseMessageReceived);
 * 
 * // Open the connection.
 * anOutputChannel.openConnection();
 * 
 * ...
 * 
 * // Send a message to all receivers which have joined
 * // the multicast group udp://234.5.6.7:8043/.
 * anOutputChannel.sendMessage("Hello");
 * 
 * ...
 * // Close connection.
 * anOutputChannel.closeConnection();
 * 
 * 
 * // Handling of received message.
 * private void onResponseMessageReceived(object sender, DuplexChannelMessageEventArgs e)
 * {
 *     string aMessage = (string)e.Message;
 *     ....
 * }
 * 
 * private EventHandler&lt;DuplexChannelMessageEventArgs&gt; myOnResponseMessageReceived = new EventHandler&ltDuplexChannelMessageEventArgs&gt;()
 * {
 *     {@literal @}Override
 *     public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
 *     {
 *         onResponseMessageReceived(sender, e);
 *     }
 * };
 * </code>
 * </pre>
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
            myMulticastLoopback = true;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates duplex output channel which can send and receive messages from the duplex input channel using UDP.
     * 
     * It can create duplex output channels for unicast, multicast or broadcast communication.
     * If the property UnicastCommunication is set to true then it creates the output channel for the unicast communication.
     * It means it can send messages to one particular input channel and receive messages only from that input channel.
     * If the property UnicastCommunication is set to false then it creates the output channel for mulitcast or broadcast communication.
     * It means it can send mulitcast or broadcast messages which can be received by multiple input channels.
     * It can also receive multicast and broadcast messages.<br/>
     * <br/>
     * Creating the duplex output channel for unicast communication (e.g. for client-service communication).
     * <pre>
     * <code>
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8765/");
     * </code>
     * </pre>
     * Creating the duplex output channel for sending mulitcast messages (e.g. for streaming video to multiple receivers).
     * <pre>
     * <code>
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *    // Setup the factory to create channels for mulitcast or broadcast communication.
     *    .setUnicastCommunication(false);
     * 
     * // Create output channel which will send messages to the mulitcast group 234.4.5.6 on the port 8765.
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://234.4.5.6:8765/");
     * </code>
     * </pre>
     * Creating the duplex output channel for sending broadcast messages.
     * <pre>
     * <code>
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *    // Setup the factory to create channels for mulitcast or broadcast communication.
     *    .setUnicastCommunication(false)
     *    // Setup the factory to create chennels which are allowed to send broadcast messages.
     *    .setAllowSendingBroadcasts(true);
     * 
     * // Create output channel which will send broadcast messages to the port 8765.
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://255.255.255.255:8765/");
     * </code>
     * </pre>
     * @param channelId UDP address in a valid URI format e.g. udp://127.0.0.1:8090/
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
     * Creates duplex output channel which can send and receive messages from the duplex input channel using UDP.
     * 
     * It can create duplex output channels for unicast, multicast or broadcast communication.
     * If the property UnicastCommunication is set to true then it creates the output channel for the unicast communication.
     * It means it can send messages to one particular input channel and receive messages only from that input channel.
     * If the property UnicastCommunication is set to false then it creates the output channel for mulitcast or broadcast communication.
     * It means it can send mulitcast or broadcast messages which can be received by multiple input channels.
     * It can also receive multicast and broadcast messages.<br/>
     * <br/>
     * This method allows to specify the id of the created output channel.<br/>
     * Creating the duplex output channel for unicast communication (e.g. for client-service communication)
     * with a specific output channel id.
     * <pre>
     * <code>
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
     * String aSessionId = UUID.randomUUID().toString();
     * IDuplexOutputChannel anOutputChannel = aMessaging.CreateDuplexOutputChannel("udp://127.0.0.1:8765/", aSessionId);
     * </code>
     * </pre>
     * Creating the duplex output channel which can send messages to a particular UDP address and
     * which can recieve messages on a specific UDP address and which can receive mulitcast messages.
     * <pre>
     * <code>
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *    // Setup the factory to create channels for mulitcast or broadcast communication.
     *    .setUnicastCommunication(false)
     *    // Specify the mulitcast group to receive messages from.
     *    .setMulticastGroupToReceive("234.4.5.6");
     * 
     * // Create output channel which can send messages to the input channel listening to udp://127.0.0.1:8095/
     * // and which is listening to udp://127.0.0.1:8099/ and which can also receive messages sent for the mulitcast
     * // group 234.4.5.6.
     * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8095/", "udp://127.0.0.1:8099/");
     * </code>
     * </pre>
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. udp://127.0.0.1:8090/
     * @param responseReceiverId Unique identifier of the output channel.<br/>
     *  In unicast communication the identifier can be a string e.g. GUID which represents the session between output and input channel.<br/>
     *  In mulitcast or broadcast communication the identifier must be a valid URI address which will be used by the output channel
     *  to receive messages from input channels.<br/>
     *  <br/>
     *  If the parameter is null then in case of unicast communication a unique id is generated automatically.
     *  In case of multicast or broadcast communication the address udp://0.0.0.0:0/ is used which means the the output channel will
     *  listen to random free port on all available IP addresses.
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
     * Creates the duplex input channel which can receive and send messages to the duplex output channel using UDP.
     * 
     * It can create duplex input channels for unicast, multicast or broadcast communication.
     * If the property UnicastCommunication is set to true then it creates the input channel for the unicast communication.
     * It means, like a service it can receive connections and messages from multiple output channels but
     * send messages only to particular output channels which are connected.
     * If the property UnicastCommunication is set to false then it creates the output channel for mulitcast or broadcast communication.
     * It means it can send mulitcast or broadcast messages which can be received by multiple output channels.
     * It also can receive multicast and broadcast messages.<br/>
     * <br/>
     * Creating the duplex input channel for unicast communication.
     * <pre>
     * <code>
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
     * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8765/");
     * </code>
     * </pre>
     * Creating the duplex input channel for multicast communication.
     * <pre>
     * <code>
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *    // Setup the factory to create channels for mulitcast or broadcast communication.
     *    .setUnicastCommunication(false)
     *    // Specify the mulitcast group to receive messages from.
     *    .setMulticastGroupToReceive("234.4.5.6");
     * 
     * // Create duplex input channel which is listening to udp://127.0.0.1:8095/ and can also receive multicast messages
     * // sent to udp://234.4.5.6:8095/.
     * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
     * </code>
     * </pre>
     * Sending mulitcast and broadcast messages from the duplex input channel.
     * <pre>
     * <code>
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * IMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *    // Setup the factory to create channels for mulitcast or broadcast communication.
     *    .setUnicastCommunication(false)
     *    // Setup the factory to create chennels which are allowed to send broadcast messages.
     *    .setAllowSendingBroadcasts(true);
     * 
     * // Create duplex input channel which is listening to udp://127.0.0.1:8095/.
     * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
     * 
     * // Subscribe to handle messages.
     * anIputChannel.messageReceived().subscribe(myOnMessageReceived);
     * 
     * // Start listening.
     * anIputChannel.startListening();
     * 
     * ...
     * 
     * // Send a multicast message.
     * // Note: it will be received by all output and input channels which have joined the multicast group 234.4.5.6
     * // and are listening to the port 8095.
     * anInputChannel.sendResponseMessage("udp://234.4.5.6:8095/", "Hello");
     * 
     * ...
     * 
     * // Send a broadcast message.
     * // Note: it will be received by all output and input channels within the sub-network which are listening to the port 8095.
     * anInputChannel.sendResponseMessage("udp://255.255.255.255:8095/", "Hello");
     * 
     * ...
     * 
     * // Stop listening.
     * anInputChannel.stopListening();
     * </code>
     * </pre>
     * @param channelId Identifies this duplex input channel. The channel id must be a valid URI address (e.g. udp://127.0.0.1:8090/) the input channel will listen to.
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
    
    /**
     * Sets whether the communication is unicast.
     * 
     * If true the factory will create channels for unicast communication. 
     * The unicast is the communication between one sender and one receiver. It means if a sender sends a message it is
     * routed to one particular receiver. The client-service communication is an example of the unicast communication.
     * When the client sends a request message it is delivered to one service. Then when the service sends a response message
     * it is delivered to one client.<br/>
     * If false the factory will create channels for multicast or broadcast communication which is the communication between
     * one sender and several receivers. It means when a sender sends a mulitcast or a broadcast message the message may be
     * delivered to multiple receivers. E.g. in case of video streaming the sender does not send data packets individually to
     * each receiver but it sends it just ones and routers multiply it and deliver it to all receivers.
     * 
     * @param isUnicast if true the communication is unicast. If false the communication is multicast or broadcast.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setUnicastCommunication(boolean isUnicast)
    {
        myUnicastCommunication = isUnicast;
        return this;
    }
    
    /**
     * Gets whether the communication is unicast.
     * 
     * The unicast is the communication between one sender and one receiver. It means if a sender sends a message it is
     * routed to one particular receiver. The client-service communication is an example of the unicast communication.
     * When the client sends a request message it is delivered to one service. Then when the service sends a response message
     * it is delivered to one client.<br/>
     * If false the factory will create channels for multicast or broadcast communication which is the communication between
     * one sender and several receivers. It means when a sender sends a mulitcast or a broadcast message the message may be
     * delivered to multiple receivers. E.g. in case of video streaming the sender does not send data packets individually to
     * each receiver but it sends it just ones and routers multiply it and deliver it to all receivers.
     * 
     * @return true if the communication is unicast. It returns false if the communication is multicast or broadcast.
     */
    public boolean getUnicastCommunication()
    {
        return myUnicastCommunication;
    }
    
    /**
     * Sets time to live value for UDP datagrams.
     * 
     * When an UDP datagram is traveling across the network each router decreases its TTL value by one.
     * Once the value is decreased to 0 the datagram is discarded. Therefore the TTL value specifies
     * how many routers a datagram can traverse.<br/>
     * E.g. if the value is set to 1 the datagram will not leave the local network.<br/>
     * The default value is 128.
     * 
     * @param ttl number of routers the datagram can travel.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setTtl(int ttl)
    {
        myTtl = ttl;
        return this;
    }
    
    /**
     * Gets time to live value for UDP datagrams.
     * 
     * When an UDP datagram is traveling across the network each router decreases its TTL value by one.
     * Once the value is decreased to 0 the datagram is discarded. Therefore the TTL value specifies
     * how many routers a datagram can traverse.<br/>
     * E.g. if the value is set to 1 the datagram will not leave the local network.<br/>
     * The default value is 128.
     * 
     * @return number of routers the datagram can travel.
     */
    public int getTtl()
    {
        return myTtl;
    }
    
    /**
     * Sets the multicast group to receive messages from.
     * 
     * Multicast group (multicast address) is a an IP address which is from the range 224.0.0.0 - 239.255.255.255.
     * (The range from 224.0.0.0 to 224.0.0.255 is reserved for low-level routing protocols and you should not use it in your applications.)
     * Receiving messages from the mulitcast group means the communication is not unicast but mulitcast.
     * Therefore to use this property UnicastCommunication must be set to false.<br/>
     * <br/>
     * Creating input channel which can receive multicast messages.
     * <pre>
     * <code>
     * // Create UDP messaging factory using simple protocol formatter.
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *     // Setup messaging factory to create channels for mulitcast or broadcast communication.
     *     .setUnicastCommunication(false)
     *     // Set the multicast group which shall be joined for receiving messages.
     *     .setMulticastGroupToReceive("234.5.6.7");
     * 
     * // Create input channel which will listen to udp://192.168.30.1:8043/ and which will also
     * // receive messages from the multicast group udp://234.5.6.7:8043/.
     * IInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://192.168.30.1:8043/");
     * </code>
     * </pre>
     * Creating output channel which can send multicast messages.
     * <pre>
     * <code>
     * // Create UDP messaging factory using simple protocol formatter.
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter)
     *     // Setup messaging factory to create channels for mulitcast or broadcast communication.
     *     .setUnicastCommunication(false);
     * 
     * // Create output channel which can send messages to the multicast address udp://234.5.6.7:8043/.
     * IOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://234.5.6.7:8043/");
     * </code>
     * </pre>
     * 
     * @param multicastGroup multicast group which shall be joined e.g. "234.5.6.7"
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setMulticastGroupToReceive(String multicastGroup)
    {
        myMulticastGroupToReceive = multicastGroup;
        return this;
    }
    
    /**
     * Gets the multicast group to receive messages from.
     * @return multicast group
     */
    public String getMulticastGroupToReceive()
    {
        return myMulticastGroupToReceive;
    }
    
    /**
     * Enables / disables sending broadcast messages.
     * 
     * Broadcast is a message which is routed to all devices within the sub-network.
     * To be able to send broadcasts UnicastCommunication must be set to false.<br/>
     * <br/>
     * Output channel which can send broadcast messages to all input channels within the sub-network
     * which listen to the port 8055.
     * <pre>
     * <code>
     * // Create UDP messaging factory using simple protocol formatter.
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter);
     * 
     * // Setup messaging factory to create channels for mulitcast or broadcast communication.
     * aMessaging.setUnicastCommunication(false);
     * 
     * // Enable sending broadcasts.
     * aMessaging.setAllowSendingBroadcasts(true);
     * 
     * // Create output channel which will send broadcast messages to all devices within the sub-network
     * // which listen to the port 8055.
     * IOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://255.255.255.255:8055/");
     * 
     * // Initialize output channel for sending broadcast messages and receiving responses.
     * anOutputChannel.openConnection();
     * 
     * // Send UDP broadcast.
     * anOutputChannel.sendMessage("Hello");
     * 
     * ...
     * 
     * // Close channel - it will release listening thread.
     * anOutputChannel.closeConnection();
     * </code>
     * </pre>
     * Input channel which can receive broadcast messages.
     * <pre>
     * <code>
     * // Create UDP messaging factory using simple protocol formatter.
     * IProtocolFormatter aProtocolFormatter = new EasyProtocolFormatter();
     * UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(aProtocolFormatter);
     * 
     * // Setup messaging factory to create channels for mulitcast or broadcast communication.
     * aMessaging.setUnicastCommunication(false);
     * 
     * // Create input channel which can receive broadcast messages to the port 8055.
     * IInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://0.0.0.0:8055/");
     * 
     * // Subscribe to receive messages.
     * anInputChannel.messageReceived().subscribe(myOnMessageReceived);
     * 
     * // Start listening for messages.
     * anInputChannel.startListening();
     * 
     * ...
     * 
     * // Stop listening.
     * anInputChannel.stopListening();
     * </code>
     * </pre>
     * 
     * @param allowBroadcasts tru if sending of broadcasts is allowed.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setAllowSendingBroadcasts(boolean allowBroadcasts)
    {
        myAllowSendingBroadcasts = allowBroadcasts;
        return this;
    }
    
    /**
     * Gets whether sending of broadcasts is allowed.
     * @return true if sending of broadcasts is allowed.
     */
    public boolean getAllowSendingBroadcasts()
    {
        return myAllowSendingBroadcasts;
    }
    
    /**
     * Enables /disables receiving multicast messages from the same IP address from which they were sent.
     * 
     * In case the sender sends a message to the same multicast group and port as itself has joined this value
     * specifies whether it shall also receive the message or not. It means if it shall send the message to itself.
     * If the value is true then yes the message will be delivered to sender too.
     * Default value is true.
     * 
     * @param allowMulticastLoopback true if the message shall be delivered to sender too.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setMulticastLoopback(boolean allowMulticastLoopback)
    {
        myMulticastLoopback = allowMulticastLoopback;
        return this;
    }
    
    /**
     * Returns whether the sender can receive the multicast message which sent in itself.
     * @return true if it can receive it.
     */
    public boolean getMulticastLoopback()
    {
        return myMulticastLoopback;
    }
    
    /**
     * Sets or gets the port which shall be used for receiving response messages by output channel in case of unicast communication.
     * 
     * When a client connects an IP address and port for the unicast communication a random free port is assigned for receiving messages.
     * This property allows to use a specific port instead of random one.
     * This property works only for the unicast communication.<br/>
     * <br/>
     * Default value is -1 which means a random free port is chosen for receiving response messages.
     * 
     * @param port port number which shall be used for receiving response messages.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setResponseReceiverPort(int port)
    {
        myResponseReceiverPort = port;
        return this;
    }
    
    /**
     * Returns port number which shall be used for receiving response messages in unicast communication.
     * 
     * Default value is -1 which means a random free port is chosen for receiving response messages.
     * 
     * @return port number which shall be used for receiving response messages.
     */
    public int getResponseReceiverPort()
    {
        return myResponseReceiverPort;
    }
    
    
    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return dispatcher providing the threading mode.
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return dispatcher providing the threading mode.
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    /**
     * Sets the flag indicating whether the socket can be bound to the address which is already used.
     * @param allowReuseAddressFlag true if the socket can bound address and port which is already in use.
     * @return instance of this UdpMessagingSystemFactory.
     */
    public UdpMessagingSystemFactory setReuseAddress(boolean allowReuseAddressFlag)
    {
        myReuseAddress = allowReuseAddressFlag;
        return this;
    }
    
    /**
     * Gets the flag indicating whether the socket can be bound to the address which is already used.
     * @return true if the socket can bound address and port which is already in use.
     */
    public boolean getReuseAddress()
    {
        return myReuseAddress;
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
