/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright � Ondrej Uzovic 2015
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.io.Serializable;

/**
 * Internal message for interaction with the message bus.
 *
 */
public class MessageBusMessage implements Serializable
{
    /**
     * Default constructor available for deserialization.
     */
    public MessageBusMessage()
    {
    }
    
    /**
     * Constructs the message.
     * @param request Requested from the message bus.
     * @param id Depending on type of the request it is either client id or service id.
     * @param messageData If the request is SendRequestMessage or SendResponseMessage it is the serialized message data.
     */
    public MessageBusMessage(EMessageBusRequest request, String id, Object messageData)
    {
        Request = request;
        Id = id;
        MessageData = messageData;
    }
    
    /**
     * Request for the message bus.
     */
    public EMessageBusRequest Request;

    /**
     * Depending on the request it is either client id or service id.
     */
    public String Id;
    
    /**
     * If the request is SendRequestMessage or SendResponseMessage it contains the serialized message data
     * which is sent between client and service. 
     * Otherwise it is null.
     */
    public Object MessageData;
    
    private static final long serialVersionUID = -270022076132548207L;
}
