/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Specifies the broker request.
 * The request for the broker is the message that is intended for the broker and not for the subscribers.
 * This message is used by the broker client to subscribe and unsubscribe.
 *
 */
public enum EBrokerRequest
{
    /**
     * Request to subscribe exactly for the specified message.
     */
    Subscribe,
    
    /**
     * Request to subscribe for message type ids that match with the regular expression.
     * I.e. regular expression is used to identify what message types shall be notified
     * to the client.
     */
    SubscribeRegExp,
    
    /**
     * Request to unsubscribe from exactly specified message.
     */
    Unsubscribe,
    
    /**
     * Request to unsubscribe the regular expression.
     */
    UnsubscribeRegExp,
    
    /**
     * Request to unsubscribe all messages and regular expressions.
     */
    UnsubscribeAll
}
