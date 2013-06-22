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
 * Internal message used between DuplexBroker and DuplexBrokerClient.
 *
 */
public class BrokerMessage implements Serializable
{
    /**
     * Default constructor used for serialization/deserialization.
     */
    public BrokerMessage()
    {
    }
    
    /**
     * Constructs the message requesting the broker to subscribe or unsubscribe events.
     * @param request subscribe or unsubscribe request
     * @param messageTypes message types that shall be subscribed or unsubscribed
     */
    public BrokerMessage(EBrokerRequest request, String[] messageTypes)
    {
        Request = request;
        MessageTypes = messageTypes;
        Message = null;
    }
    
    /**
     * Constructs the broker message requesting the broker to publish an event.
     * @param messageTypeId message type that shall be published.
     * @param message serialized message to be published.
     */
    public BrokerMessage(String messageTypeId, Object message)
    {
        Request = EBrokerRequest.Publish;
        MessageTypes = new String[] { messageTypeId };
        Message = message;
    }

    /**
     * Type of the request.
     */
    public EBrokerRequest Request;
    
    /**
     * Array of message types.
     */
    public String[] MessageTypes;
    
    /**
     * Serialized message that shall be notified to subscribers.
     */
    public Object Message;
    
    private static final long serialVersionUID = -7632473220961947955L;
}
