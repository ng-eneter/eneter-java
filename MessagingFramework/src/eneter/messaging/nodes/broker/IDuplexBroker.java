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
 * The interface declares the broker.
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
     * Subscribes for event ids matching with the given regular expression.
     * 
     * When a published message comes to the broker, the broker will check the message type id
     * and will forward it to all subscribed clients.<br/>
     * The broker will use the given regular expression to recognize whether the client is subscribed
     * or not.<br/>
     * The .NET based Broker internally uses Regex class to evaluate the regular expression.<br/>
     * The Java based Broker internally uses Pattern class to evaluate the regular expression.<br/>
     * Regular expressions between .NET and Java does not have to be fully compatible.
     * <pre>
     * Few examples for subscribing via regular expression.
     * {@code
     * // Subscribing for message types starting with the string MyMsg.Speed
     * myDuplexBrokerClient.subscribeRegExp(@"^MyMsg\.Speed.*);
     *
     * // Subscribing for message types starting with MyMsg.Speed or App.Utilities
     * myDuplexBrokerClient.SubscribeRegExp(@"^MyMsg\.Speed.*|^App\.Utilities.*");
     * }
     * </pre>
     * 
     * @param regularExpression Regular expression that will be evaluated by the broker to recognize whether the client is subscribed.
     * @throws Exception
     */
    void subscribeRegExp(String regularExpression) throws Exception;
    
    /**
     * Subscribes for message ids matching with the given list of regular expressions.
     * 
     * When a published message comes to the broker, the broker will check the message type id
     * and will forward it to all subscribed clients.<br/>
     * The broker will use the given regular expression to recognize whether the client is subscribed
     * or not.<br/>
     * The .NET based broker internally uses Regex class provided by .NET.<br/>
     * The Java based broker internally uses Pattern class provided by .NET.<br/>
     * Regular expressions between .NET and Java does not have to be fully compatible.
     * @param regularExpressions
     * @throws Exception
     */
    void subscribeRegExp(String[] regularExpressions) throws Exception;
    
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
     * Removes the regular expression subscription.
     * @param regularExpression Regular expression that was previously used for the subscription and now shall be removed.
     * @throws Exception
     */
    void unsubscribeRegExp(String regularExpression) throws Exception;
    
    /**
     * Removes regular expression subscriptions.
     * 
     * When the broker receives this request, it will search if the given regular expression strings
     * exist for the calling client. If yes, they will be removed.
     * @param regularExpressions Regular expressions that shall be removed from subscriptions.
     * @throws Exception
     */
    void unsubscribeRegExp(String[] regularExpressions) throws Exception;
    
    /**
     * Completely unsubscribes the client from all messages (including all regular expressions).
     * @throws Exception
     */
    void unsubscribe() throws Exception;
}
