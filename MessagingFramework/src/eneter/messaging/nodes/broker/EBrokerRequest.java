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
    Subscribe(10),
    
    /**
     * Request to unsubscribe from exactly specified message.
     */
    Unsubscribe(20),
    
    /**
     * Request to unsubscribe all messages and regular expressions.
     */
    UnsubscribeAll(30),
    
    /**
     * Request to publish a message.
     */
    Publish(40);
    
    /**
     * Returns value of the enumeration type.
     * @return
     */
    public int geValue()
    {
        return myValue;
    }
    
    public static EBrokerRequest fromInt(int i)
    {
        switch (i)
        {
            case 10: return Subscribe;
            case 20: return Unsubscribe;
            case 30: return UnsubscribeAll;
            case 40: return Publish;
        }
        return null;
    }
    
    private EBrokerRequest(int value)
    {
        myValue = value;
    }

    private final int myValue;
}
