/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.io.Serializable;

/**
 * The data representing the message sent to the broker to notify subscribed clients.
 * @author Ondrej Uzovic
 *
 */
public class BrokerNotifyMessage implements Serializable
{
    /**
     * Default constructor used for the deserialization.
     */
    public BrokerNotifyMessage()
    {
    }
    
    /**
     * Constructs the message data from the input parameters.
     * @param messageTypeId Type of the notified message.
     * @param message Message content.
     */
    public BrokerNotifyMessage(String messageTypeId, Object message)
    {
        MessageTypeId = messageTypeId;
        Message = message;
    }
    
    
    /**
     * Type of the notified message.
     */
    public String MessageTypeId;
    
    /**
     * Serialized message that shall be notified to subscribers.
     */
    public Object Message;
    
    private static final long serialVersionUID = 2939701088460435251L;
}
