/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit.BufferedMessagingFactory;
import eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit.MonitoredMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * This messaging combines buffered and monitored messaging.
 * 
 * Monitored messaging constantly monitors the connection and if the disconnection is detected the buffered messaging
 * is notified. Buffered messaging then tries to reconnect and meanwhile stores all sent messages into a buffer.
 * Once the connection is recovered the messages stored in the buffer are sent.<br/>
 * <br/>
 * The buffered monitored messaging is composed from following messagings:
 * <ul>
 * <li><i>BufferedMessaging</i> is on the top and is responsible for storing of messages during the disconnection (offline time)
 * and automatic reconnect.</li>
 * <li><i>MonitoredMessaging</i> is in the middle and is responsible for continuous monitoring of the connection.</li>
 * <li><i>UnderlyingMessaging</i> (e.g. TCP) is on the bottom and is responsible for sending and receiving messages.</li>
 * </ul>
 * The following example shows how to create BufferedMonitoredMessaging:
 * <pre>
 * {@code
 * // Create TCP messaging system.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 * 
 * // Create buffered monitored messaging which takes TCP as underlying messaging.
 * IMessagingSystemFactory aMessaging = new BufferedMonitoredMessagingFactory(anUnderlyingMessaging);
 * 
 * // Then creating channels which can be then attached to communication components.
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8095/");
 * IDuplexInputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8095/");
 * }
 * </pre>
 */
public class BufferedMonitoredMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory with default settings.
     * 
     * The maximum offline time for buffered messaging is set to 10 seconds.
     * The ping frequency for monitored messaging is set to 1 second and the receive timeout for monitored messaging
     * is set to 2 seconds.
     * 
     * @param underlyingMessaging underlying messaging system e.g. TCP, ...
     */
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging)
    {
        this(underlyingMessaging,
                10000, // max offline time
                1000, 2000);
    }
    
    /**
     * Constructs the factory with the specified parameters.
     * 
     * 
     * @param underlyingMessaging underlying messaging system e.g. TCP, ...
     * @param maxOfflineTime the maximum time, the messaging can work offline. When the messaging works offline,
     *      the sent messages are buffered and the connection is being reopened. If the connection is
     *      not reopen within maxOfflineTime, the connection is closed.
     * @param pingFrequency how often the connection is checked with the 'ping' requests.
     * @param pingResponseTimeout the maximum time, the response for the 'ping' is expected.
     */
    public BufferedMonitoredMessagingFactory(IMessagingSystemFactory underlyingMessaging,
            // Buffered Messaging
            long maxOfflineTime,

            // Monitored Messaging
            long pingFrequency,
            long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMonitoredMessaging = new MonitoredMessagingFactory(underlyingMessaging, pingFrequency, pingResponseTimeout);
            myBufferedMessaging = new BufferedMessagingFactory(myMonitoredMessaging, maxOfflineTime);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * Creates the output channel which can send and receive messages.
     * 
     * This duplex output channel provides the buffered messaging and the connection monitoring.
     * It regularly checks if the connection is available. It sends 'ping' requests and expects 'ping' responses
     * within the specified time. If the 'ping' response does not come the disconnection is notified to the buffered messaging.
     * The buffered messaging then tries to reconnect and meanwhile stores the sent messages to the buffer.
     * Once the connection is recovered the messages stored in the buffer are sent.
     * If the connection recovery was not possible the event IDuplexOutputChannel.connectionClosed()
     * is raised the message buffer is deleted.
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
     * Creates the output channel which can send and receive messages.
     * 
     * This duplex output channel provides the buffered messaging and the connection monitoring.
     * It regularly checks if the connection is available. It sends 'ping' requests and expects 'ping' responses
     * within the specified time. If the 'ping' response does not come the disconnection is notified to the buffered messaging.
     * The buffered messaging then tries to reconnect and meanwhile stores the sent messages to the buffer.
     * Once the connection is recovered the messages stored in the buffer are sent.
     * If the connection recovery was not possible the event IDuplexOutputChannel.connectionClosed()
     * is raised the message buffer is deleted.
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
     * Creates the input channel which can receive and send messages.
     * 
     * This duplex input channel provides the buffered messaging and the connection monitoring.
     * It regularly checks if the duplex output channel is still connected. It expect, that every connected duplex output channel
     * sends regularly 'ping' messages. If the 'ping' message from the duplex output channel is not received within the specified
     * time the duplex output channel is disconnected and the buffered messaging is notified about the disconnection.
     * The buffered messaging then puts all sent response messages to the buffer and waits whether the duplex output channel reconnects.
     * If the duplex output channel reopens the connection the messages stored in the buffer are sent.
     * If the duplex output channel does not reconnect the event
     * IDuplexInputChannel.responseReceiverDisconnected() is raised and response messages are deleted from the buffer.
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

    /**
     * Returns underlying buffered messaging.
     * @return buffered messaging
     */
    public BufferedMessagingFactory getBufferedMessaging()
    {
        return myBufferedMessaging;
    }
    
    /**
     * Returns underlying monitored messaging.
     * @return monitored messaging
     */
    public MonitoredMessagingFactory getMonitoredMessaging()
    {
        return myMonitoredMessaging;
    }
    
    
    private BufferedMessagingFactory myBufferedMessaging;
    private MonitoredMessagingFactory myMonitoredMessaging;
}
