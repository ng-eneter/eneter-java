/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.net.system.Event;

/**
 * Broker component (for publish-subscribe scenarios).
 * The broker receives messages and forwards them to subscribed clients.
 *
 */
public interface IDuplexBroker extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the observed message is received.
     * @return
     */
    Event<BrokerMessageReceivedEventArgs> brokerMessageReceived();
    
    /**
     * Publishes the event.
     * 
     * @param eventId identification of published event.
     * @param serializedMessage message content. If the message is not a primitive type or String then the input parameter expects the message is already serialized!
     * @throws Exception
     */
    void sendMessage(String eventId, Object serializedMessage) throws Exception;
    
    /**
     * Subscribes for the event.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param eventId identification of event that shall be observed
     * @throws Exception
     */
    void subscribe(String eventId) throws Exception;
    
    /**
     * Subscribes for list of events.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param eventIds list of message types the client wants to observe
     * @throws Exception
     */
    void subscribe(String[] eventIds) throws Exception;
    
    /**
     * Unsubscribes from the specified event.
     * @param eventId event the client does not want to observe anymore
     * @throws Exception
     */
    void unsubscribe(String eventId) throws Exception;
    
    /**
     * Unsubscribes from specified events.
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
