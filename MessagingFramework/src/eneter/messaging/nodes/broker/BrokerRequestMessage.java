/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * The class represents the data structure used to send requests to the broker.
 * The request for the broker is the message that is intended for the broker and not for the subscribers.
 * This message is used by the broker client to subscribe and unsubscribe.
 * @author Ondrej Uzovic
 *
 */
public class BrokerRequestMessage
{
    /**
     * Default constructor used for serialization/deserialization.
     */
    public BrokerRequestMessage()
    {
    }
    
    /**
     * Creates the request from input parameters.
     * @param request subscribe or unsubscribe request
     * @param messageTypes message types that shall be subscribed or unsubscribed
     */
    public BrokerRequestMessage(EBrokerRequest request, String[] messageTypes)
    {
        myRequest = request;
        myMessageTypes = messageTypes;
    }

    /**
     * Type of the request.
     */
    public EBrokerRequest myRequest;
    
    /**
     * Array of message types.
     */
    public String[] myMessageTypes;
}
