/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;

/**
 * Broker client to publish and subscribe messages in the broker.
 * The broker client allows to publish events via the broker, so that broker will forward them to all subscribers.<br/>
 * BrokerClient also allows to subscribe for events of interest.
 *
 */
public interface IDuplexBrokerClient extends IAttachableDuplexOutputChannel
{
    /**
     * Event raised when the connection with the service was open.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Event raised when the connection with the service was closed.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * The event is invoked when the observed message is received from the broker.
     * @return
     */
    Event<BrokerMessageReceivedEventArgs> brokerMessageReceived();
    
    /**
     * Publishes the event via the broker.
     * @param eventId event identifier
     * @param serializedMessage message content. If the message is not a primitive type or String then the input parameter expects the message is already serialized!
     * @throws Exception
     */
    void sendMessage(String eventId, Object serializedMessage) throws Exception;
    
    /**
     * Subscribes the client for the event.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param eventId message type the client wants to observe
     * @throws Exception
     */
    void subscribe(String eventId) throws Exception;
    
    /**
     * Subscribes the client for list of events.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param eventIds list of events the client wants to observe
     * @throws Exception
     */
    void subscribe(String[] eventIds) throws Exception;
    
    /**
     * Unsubscribes the client from the specified event.
     * @param eventId message type the client does not want to observe anymore
     * @throws Exception
     */
    void unsubscribe(String eventId) throws Exception;
    
    /**
     * Unsubscribes the client from specified events.
     * @param eventIds list of message types the client does not want to observe anymore
     * @throws Exception
     */
    void unsubscribe(String[] eventIds) throws Exception;
    
    /**
     * Completely unsubscribes the client from all messages (including all regular expressions).
     * @throws Exception
     */
    void unsubscribe() throws Exception;
}
