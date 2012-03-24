/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit.BufferedMessagingFactory;
import eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit.MonitoredMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Extends the communication by the buffered messaging and the network connection monitoring.
 * 
 * This is the composite messaging system that consist of:
 * <ol>
 * <li>Buffered Messaging  --> buffering messages if disconnected (while automatically trying to reconnect)</li>
 * <li>Monitored Messaging --> constantly monitoring the connection</li>
 * <li>Messaging System    --> responsible for sending and receiving messages</li>
 * </ol>
 * The buffer stores messages if the connection is not open. The connection monitor constantly checks if the connection
 * is established. See also BufferedMessagingFactory and MonitoredMessagingFactory.
 *
 */
public class BufferedMonitoredMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory with default settings.
     * 
     * The serializer for the 'ping' messages checking the connection is set to XmlStringSerializer.
     * The maximum offline time is set to 10 seconds.
     * The duplex output channel will check the connection with the 'ping' once per second and the response must be received within 2 seconds.
     * Otherwise the connection is closed.<br/>
     * The duplex input channel expects the 'ping' request at least once per 2 seconds. Otherwise the duplex output
     * channel is disconnected.
     * 
     * @param underlyingMessaging underlying messaging system e.g. HTTP, TCP, ...
     */
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, new XmlStringSerializer(),
                10000, // max offline time
                1000, 2000);
    }
    
    /**
     * Constructs the factory with the specified parameters.
     * 
     * The maximum offline time is set to 10 seconds.
     * The duplex output channel will check the connection with the 'ping' once per second and the response must be received within 2 seconds.
     * Otherwise the connection is closed.<br/>
     * The duplex input channel expects the 'ping' request at least once per 2 seconds. Otherwise the duplex output
     * channel is disconnected.
     * 
     * @param underlyingMessaging underlying messaging system
     * @param serializer
     */
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging, ISerializer serializer)
    {
        this(underlyingMessaging, serializer,
                10000, // max offline time
                1000, 2000);
    }
    
    /**
     * Constructs the factory with the specified parameters.
     * 
     * 
     * @param underlyingMessaging underlying messaging system
     * @param serializer serializer used to serialize the 'ping' requests
     * @param maxOfflineTime the maximum time, the messaging can work offline. When the messaging works offline,
     *      the sent messages are buffered and the connection is being reopened. If the connection is
     *      not reopen within maxOfflineTime, the connection is closed.
     * @param pingFrequency how often the connection is checked with the 'ping' requests.
     * @param pingResponseTimeout the maximum time, the response for the 'ping' is expected.
     */
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            ISerializer serializer,

            // Buffered Messaging
            long maxOfflineTime,

            // Monitored Messaging
            long pingFrequency,
            long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IMessagingSystemFactory aMonitoredMessaging = new MonitoredMessagingFactory(underlyingMessaging, serializer, pingFrequency, pingResponseTimeout);
            myBufferedMessaging = new BufferedMessagingFactory(aMonitoredMessaging, maxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * Creates the output channel sending messages to the input channel.
     * 
     * This output channel provides the buffered messaging. Since the the output channel does not maintain open
     * connection, the connection monitoring is not applicable.<br/>
     * If the input channel is not available, the sent messages are stored in the buffer from where they are sent again
     * when the input channel is available.
     * If the message is not sent from the buffer (because the input channel is not available) within the specified offline time,
     * the message is removed from the buffer.
     * <br/>
     * Notice, when the message was successfully sent, it does not mean the message was delivered. It still can be lost on the way.
     * 
     * @return composit output channel ICompositeOutputChannel
     */
    @Override
    public IOutputChannel createOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myBufferedMessaging.createOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the input channel receiving messages from the output channel.
     * 
     * The buffering as well as the connection monitoring are not applicable.
     * This method just uses the underlying messaging system to create the input channel.
     * 
     */
    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myBufferedMessaging.createInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * This duplex output channel provides the buffered messaging and the connection monitoring.
     * The channel regularly checks if the connection is available. It sends 'ping' requests and expects 'ping' responses
     * within the specified time. If the 'ping' response does not come, the buffered messaging layer is notified,
     * that the connection was interrupted.
     * The buffered messaging then tries to reconnect and stores sent messages to the buffer.
     * If the connection is open, the buffered messages are sent.
     * If the reconnection was not successful, it notifies IDuplexOutputChannel.connectionClosed()
     * and deletes messages from the buffer.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myBufferedMessaging.createDuplexOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * This duplex output channel provides the buffered messaging and the connection monitoring.
     * The channel regularly checks if the connection is available. It sends 'ping' requests and expects 'ping' responses
     * within the specified time. If the 'ping' response does not come, the buffered messaging layer is notified,
     * that the connection was interrupted.
     * The buffered messaging then tries to reconnect and stores sent messages to the buffer.
     * If the connection is open, the buffered messages are sent.
     * If the reconnection was not successful, it notifies IDuplexOutputChannel.connectionClosed()
     * and deletes messages from the buffer.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myBufferedMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending the response messages.
     * 
     * This duplex input channel provides the buffered messaging and the connection monitoring.
     * The channel regularly checks if the duplex output channel is still connected. It expect, that every connected duplex output channel
     * sends regularly 'ping' messages. If the 'ping' message from the duplex output channel is not received within the specified
     * time, the duplex output channel is disconnected and the buffered messaging (as the layer above) is notified about the
     * disconnection.
     * The buffered messaging then puts all sent response messages to the buffer and waits whether the duplex output channel reconnects.
     * If the duplex output channel reopens the connection, the buffered response messages are sent.
     * If the duplex output channel does not reconnect, the event
     * IDuplexInputChannel.responseReceiverDisconnected() is invoked and response messages are deleted from the buffer.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myBufferedMessaging.createDuplexInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myBufferedMessaging;
}
