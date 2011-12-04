/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.net.system.*;

/**
 * Declares the broker client.
 * The broker client allows to send messages via the broker, so that broker will forward them to all subscribers.<br/>
 * It also allows to subscribe for messages the client is interested to.
 * @author Ondrej Uzovic
 *
 */
public interface IDuplexBrokerClient extends IAttachableDuplexOutputChannel
{
    /**
     * The event is invoked when the observed message is received from the broker.
     * @return
     */
    Event<BrokerMessageReceivedEventArgs> brokerMessageReceived();
    
    /**
     * Sends the message of the specified type to the broker.
     * @param messageTypeId message type id
     * @param serializedMessage message content. If the message is not a primitive type or String then the input parameter expects the message is already serialized!
     * @throws Exception
     */
    void sendMessage(String messageTypeId, Object serializedMessage) throws Exception;
    
    /**
     * Subscribes the client for the message.
     * @param messageType message type the client wants to observe
     * @throws Exception
     */
    void subscribe(String messageType) throws Exception;
    
    /**
     * Subscribes the client for list of messages.
     * @param messageTypes list of message types the client wants to observe
     * @throws Exception
     */
    void subscribe(String[] messageTypes) throws Exception;
    
    /**
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
     * Subscribes the client for message types matching with the given list of regular expressions.
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
     * Unsubscribes the client from the specified message.
     * @param messageType message type the client does not want to observe anymore
     * @throws Exception
     */
    void unsubscribe(String messageType) throws Exception;
    
    /**
     * Unsubscribes the client from specified messages.
     * @param messageTypes list of message types the client does not want to observe anymore
     * @throws Exception
     */
    void unsubscribe(String[] messageTypes) throws Exception;
    
    /**
     * Removes the regular expression subscription.
     * @param regularExpression Regular expression that was previously used for the subscription and now shall be removed.
     * @throws Exception
     */
    void unsubscribeRegExp(String regularExpression) throws Exception;
    
    /**
     * Removes all regular expression subscriptions.
     * When the broker receives this request, it will search if the given regular expression strings
     * exist for the calling client. If yes, they will be removed.
     * @param regularExpressions
     * @throws Exception
     */
    void unsubscribeRegExp(String[] regularExpressions) throws Exception;
    
    /**
     * Completely unsubscribes the client from all messages (including all regular expressions).
     * @throws Exception
     */
    void unsubscribe() throws Exception;
}