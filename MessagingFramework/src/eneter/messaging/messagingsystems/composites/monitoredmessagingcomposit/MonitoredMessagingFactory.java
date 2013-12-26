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
 * Provides the messaging system which monitors the connection in the underlying messaging system.
 *
 *
 * When the connection is monitored, the duplex output channel periodically sends 'ping' messages
 * to the duplex input channel and waits for responses.
 * If the response comes within the specified timeout, the connection is open.
 * <br/>
 * On the receiver side, the duplex input channel waits for the 'ping' messages and monitors if the connected
 * duplex output channel is still alive. If the 'ping' message does not come within the specified timeout,
 * the particular duplex output channel is disconnected.
 * <br/><br/>
 * Notice, the output channel and the input channel do not maintain an open connection.
 * Therefore, the monitored messaging is not applicable for them. The implementation of this factory just uses
 * the underlying messaging to create them.
 * <br/><br/>
 * <b>Note</b>
 * Channels created by monitored messaging factory cannot communicate with channels, that were not created
 * by monitored factory. E.g. the channel created with the monitored messaging factory with underlying TCP
 * will not communicate with channels created directly with TCP messaging factory. The reason is, the
 * communicating channels must understand the 'ping' communication.
 *
 */
public class MonitoredMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory with default settings.
     * 
     * It uses XmlStringSerializer.
     * The duplex output channel will check the connection with the 'ping' once per second and the response must be received within 2 seconds.
     * Otherwise the connection is closed.<br/>
     * The duplex input channel expects the 'ping' request at least once per 2s. Otherwise the duplex output
     * channel is disconnected.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     */
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, new XmlStringSerializer(), 1000, 2000);
    }
    
    /**
     * Constructs the factory from specified parameters.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     * @param serializer serializer used to serialize 'ping' messages
     * @param pingFrequency how often the duplex output channel pings the connection
     * @param pingResponseTimeout For the duplex output channel: the maximum time, the response for the ping must be received
     *           For the duplex input channel: the maximum time within the ping for the connected duplex output channel
     *           must be received.
     */
    public MonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            ISerializer serializer,
            long pingFrequency,
            long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessaging;
            mySerializer = serializer;
            myPingFrequency = pingFrequency;
            myPingResponseTimeout = pingResponseTimeout;
            myResponseReceiverTimeout = pingFrequency + pingResponseTimeout;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * The channel also regularly checks if the connection is available. It sends 'ping' messages and expect 'ping' responses
     * within the specified timeout. If the 'ping' response does not come within the specified timeout, the event
     * IDuplexOutputChannel.connectionClosed is invoked.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myPingResponseTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * The channel also regularly checks if the connection is available. It sends 'ping' messages and expect 'ping' responses
     * within the specified timeout. If the 'ping' response does not come within the specified timeout, the event
     * IDuplexOutputChannel.connectionClosed" is invoked.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new MonitoredDuplexOutputChannel(anUnderlyingChannel, mySerializer, myPingFrequency, myPingResponseTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending the response messages.
     * 
     * It also checks if the duplex output channel is still connected. It expect, that every connected duplex output channel
     * sends regularly 'ping' messages. If the 'ping' message from the duplex output channel is not received within the specified
     * timeout, the duplex output channel is disconnected. The event IDuplexInputChannel.responseReceiverDisconnected
     * is invoked.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexInputChannel anUnderlyingChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new MonitoredDuplexInputChannel(anUnderlyingChannel, mySerializer, myResponseReceiverTimeout);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myUnderlyingMessaging;
    private long myPingFrequency;
    private long myPingResponseTimeout;
    private long myResponseReceiverTimeout;
    private ISerializer mySerializer;
}
