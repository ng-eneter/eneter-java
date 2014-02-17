/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Extension allowing to work offline until the connection is available.
 * 
 * 
 * The buffered messaging is intended to overcome relatively short time intervals when the connection is not available.
 * It means, the buffered messaging is able to hide the connection is not available and work offline while
 * trying to reconnect.<br/>
 * If the connection is not available, the buffered messaging stores sent messages (and sent response messages)
 * in the buffer and sends them when the connection is established.<br/>
 * Buffered messaging also checks if the between duplex output channel and duplex input channel is active.
 * If the connection is not used (messages do not flow) the buffered messaging
 * waits the specified maxOfflineTime and then disconnects the client.
 *  
 * <br/> 
 * <br/>
 * <b>Note:</b><br/>
 * The buffered messaging does not require that both communicating parts create channels with buffered messaging factory.
 * It means, e.g. the duplex output channel created with buffered messaging with underlying TCP, can send messages
 * directly to the duplex input channel created with just TCP messaging factory.
 *
 */
public class BufferedMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory with default parameters.
     * 
     * The maximum offline time will be set to 10 seconds.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     */
    public BufferedMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging, 10000);
    }
    
    /**
     * Constructs the factory from the specified parameters.
     * 
     * @param underlyingMessaging underlying messaging system e.g. Websocket, TCP, ...
     * @param maxOfflineTime the max time (in milliseconds), the communicating applications can be disconnected.
     */
    public BufferedMessagingFactory(IMessagingSystemFactory underlyingMessaging, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessaging;
            myMaxOfflineTime = maxOfflineTime;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * If the connection is not established, it puts sent messages to the buffer while trying to reconnect.
     * Then when the connection is established, the messages are sent from the buffer.
     * If the reconnect is not successful within the maximum offline time, it notifies
     * IDuplexOutputChannel.connectionClosed()
     * and messages are deleted from the buffer.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingDuplexOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new BufferedDuplexOutputChannel(anUnderlyingDuplexOutputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * 
     * If the connection is not established, it puts sent messages to the buffer while trying to reconnect.
     * Then when the connection is established, the messages are sent from the buffer.
     * If the reconnect is not successful within the maximum offline time, it notifies
     * IDuplexOutputChannel.connectionClosed()
     * and messages are deleted from the buffer.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexOutputChannel anUnderlyingDuplexOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new BufferedDuplexOutputChannel(anUnderlyingDuplexOutputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending the response messages.
     * 
     * If the connection with the duplex output channel is not established, it puts sent response messages to the buffer.
     * Then, when the duplex input channel is connected, the response messages are sent.
     * If the duplex output channel does not connect within the specified maximum offline time, the event
     * IDuplexInputChannel.responseReceiverDisconnected() is invoked and response messages are deleted from the buffer.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexInputChannel anUnderlyingDuplexInputChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new BufferedDuplexInputChannel(anUnderlyingDuplexInputChannel, myMaxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private IMessagingSystemFactory myUnderlyingMessaging;
    private long myMaxOfflineTime;
}
