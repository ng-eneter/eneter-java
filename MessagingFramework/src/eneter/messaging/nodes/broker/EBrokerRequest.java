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
     * Request to unsubscribe from exactly specified message.
     */
    Unsubscribe,
    
    /**
     * Request to unsubscribe all messages and regular expressions.
     */
    UnsubscribeAll,
    
    /**
     * Request to publish a message.
     */
    Publish
}
