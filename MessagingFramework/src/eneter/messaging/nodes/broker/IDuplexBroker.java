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
 * Broker component.
 * The broker is the communication component intended for publish-subscribe scenario.
 * It is the component which allows consumers to subscribe for desired message types
 * and allows publishers to send a message to subscribed consumers.<br/>
 * <br/>
 * When the broker receives a message from a publisher it finds all consumers subscribed to that
 * message and forwards them the message.
 *
 */
public interface IDuplexBroker extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the publisher published a message to subscribers.
     * @return event
     */
    Event<PublishInfoEventArgs> messagePublished();
    
    /**
     * The event is invoked when the broker subscribed a client for messages.
     * @return event
     */
    Event<SubscribeInfoEventArgs> clientSubscribed();
    
    /**
     * The event is invoked when the broker unsubscribed a client from messages.
     * @return event
     */
    Event<SubscribeInfoEventArgs> clientUnsubscribed();
    
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
     * @param messageType identifies the type of the message which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String messageType) throws Exception;
    
    /**
     * Subscribes for list of message types.
     * 
     * @param messageTypes list of message types which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String[] messageTypes) throws Exception;
    
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
    
    /**
     * Returns messages which are subscribed by the given subscriber.
     * @param responseReceiverId subscriber response receiver id.
     * @return array of subscribed messages
     */
    String[] getSubscribedMessages(String responseReceiverId);
    
    /**
     * Returns subscribers which are subscribed for the given message type id.
     * @param messageTypeId >message type id
     * @return array of subscribed subscribers
     */
    String[] GetSubscribedResponseReceivers(String messageTypeId);
}
