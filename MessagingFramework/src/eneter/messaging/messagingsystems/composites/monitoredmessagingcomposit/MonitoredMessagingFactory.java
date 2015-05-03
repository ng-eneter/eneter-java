/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Extension providing the connection monitoring.
 * 
 * The monitored messaging regularly monitors if the connection is still available.
 * It sends ping messages and receives ping messages in a defined frequency. If sending of the ping message fails
 * or the ping message is not received within the specified time the connection is considered broken.<br/>
 * The advantage of the monitored messaging is that the disconnection can be detected very early.<br/>
 *  <br/>
 * When the connection is monitored, the duplex output channel periodically sends 'ping' messages
 * to the duplex input channel and waits for responses.
 * If the response comes within the specified timeout, the connection is open.
 * <br/>
 * On the receiver side, the duplex input channel waits for the 'ping' messages and monitors if the connected
 * duplex output channel is still alive. If the 'ping' message does not come within the specified timeout,
 * the particular duplex output channel is disconnected.
 * <br/><br/>
 * <b>Note</b>
 * Channels created by monitored messaging factory cannot communicate with channels, that were not created
 * by monitored factory. E.g. the channel created with the monitored messaging factory with underlying TCP
 * will not communicate with channels created directly with TCP messaging factory. The reason is, the
 * communicating channels must understand the 'ping' communication.<br/>
 * 
 * <br/>
 * The following example shows how to use monitored messaging:
 * <pre>
 * // Create TCP messaging.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 * <br/>
 * // Create monitored messaging which internally uses TCP.
 * IMessagingSystemFactory aMessaging = new MonitoredMessagingSystemFactory(anUnderlyingMessaging);
 * <br/>
 * // Create the output channel.
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8045/");
 * <br/>
 * // Create message sender to send simple string messages.
 * IDuplexStringMessagesFactory aSenderFactory = new DuplexStringMessagesFactory();
 * IDuplexStringMessageSender aSender = aSenderFactory.CreateDuplexStringMessageSender();
 * <br/>
 * // Subscribe to detect the disconnection.
 * aSender.connectionClosed().subscribe(myOnConnectionClosed);
 * <br/>
 * // Subscribe to receive responses.
 * aSender.responseReceived().subscribe(myOnResponseReceived);
 * <br/>
 * // Attach output channel an be able to send messages and receive responses.
 * aSender.attachDuplexOutputChannel(anOutputChannel);
 * <br/>
 * ...
 * <br/>
 * // Send a message.
 * aSender.SendMessage("Hello.");
 * </pre>
 *
 */
public class MonitoredMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory with default settings.
     * 
     * It uses optimized custom serializer which is optimized to serialize/deserialize MonitorChannelMessage which is
     * used for the internal communication between output and input channels.
     * The ping message is sent once per second and it is expected the ping message is received at least once per two seconds.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     */
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, 1000, 2000);
    }
    
    /**
     * Constructs the factory from specified parameters.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     * @param pingFrequency how often the ping message is sent.
     * @param pingReceiveTimeout the maximum time within it the ping message must be received.
     */
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            long pingFrequency,
            long pingReceiveTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessaging;
            mySerializer = new MonitoredMessagingCustomSerializer();
            myPingFrequency = pingFrequency;
            myReceiveTimeout = pingReceiveTimeout;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the output channel which can send messages to the input channel and receive response messages.
     * 
     * In addition the output channel monitors the connection availability. It sends ping messages in a specified frequency to the input channel
     * and expects receiving ping messages within a specified time.
     * 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myReceiveTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the output channel which can send messages to the input channel and receive response messages.
     * 
     * In addition the output channel monitors the connection availability. It sends ping messages in a specified frequency to the input channel
     * and expects receiving ping messages within a specified time.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myReceiveTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the input channel which can receive messages from the output channel and send response messages.
     * 
     * In addition it expects receiving ping messages from each connected client within a specified time and sends
     * ping messages to each connected client in a specified frequency.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexInputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new MonitoredDuplexInputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myReceiveTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Sets how often the ping message shall be sent.
     * @param milliseconds time in milliseconds
     * @return this MonitoredMessagingFactory
     */
    public MonitoredMessagingFactory setPingFrequency(long milliseconds)
    {
        myPingFrequency = milliseconds;
        return this;
    }
    
    /**
     * Gets the ping frequency.
     * @return time in milliseconds.
     */
    public long getPingFrequency()
    {
        return myPingFrequency;
    }
    
    /**
     * Sets the time within it the ping message must be received.
     * @param milliseconds time in milliseconds
     * @return this MonitoredMessagingFactory
     */
    public MonitoredMessagingFactory setReceiveTimeout(long milliseconds)
    {
        myReceiveTimeout = milliseconds;
        return this;
    }
    
    /**
     * Gets the time within it the ping message must be received.
     * @return time in milliseconds.
     */
    public long getReceiveTimeout()
    {
        return myReceiveTimeout;
    }
    
    /**
     * Sets the serializer which shall be used to serialize MonitorChannelMessage.
     * @param pingSerializer serializer.
     * @return this MonitoredMessagingFactory
     */
    public MonitoredMessagingFactory setSerializer(ISerializer pingSerializer)
    {
        mySerializer = pingSerializer;
        return this;
    }
    
    /**
     * Gets the serializer which is used to serialize/deserialize MonitorChannelMessage.
     * @return serializer
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    private IMessagingSystemFactory myUnderlyingMessaging;
    private long myPingFrequency;
    private long myReceiveTimeout;
    private ISerializer mySerializer;
}
