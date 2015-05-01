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
     * @return event
     */
    Event<BrokerMessageReceivedEventArgs> brokerMessageReceived();
    
    /**
     * Publishes the message.
     * 
     * @param messageType identifies the type of the published message. The broker will forward the message
     * to all subscribers subscribed to this message type.
     * @param serializedMessage message content.
     * @throws Exception
     */
    void sendMessage(String messageType, Object serializedMessage) throws Exception;
    
    /**
     * Subscribes for the message type.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param messageType identifies the type of the message which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String messageType) throws Exception;
    
    /**
     * Subscribes for list of message types.
     * 
     * @param messageType list of message types which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String[] messageType) throws Exception;
    
    /**
     * Unsubscribes from the specified message type.
     * @param messageType message type the client does not want to receive anymore.
     * @throws Exception
     */
    void unsubscribe(String messageType) throws Exception;
    
    /**
     * Unsubscribes from specified message types.
     * @param messageTypes list of message types the client does not want to receive anymore.
     * @throws Exception
     */
    void unsubscribe(String[] messageTypes) throws Exception;
    
    /**
     * Unsubscribe all messages.
     * @throws Exception
     */
    void unsubscribe() throws Exception;
}
