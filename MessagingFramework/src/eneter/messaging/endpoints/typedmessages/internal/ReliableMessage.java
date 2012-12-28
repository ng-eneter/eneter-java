/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages.internal;

import java.io.Serializable;

public class ReliableMessage implements Serializable
{
    public enum EMessageType
    {
        Message,
        Acknowledge
    }
    
    public ReliableMessage()
    {
    }

    public ReliableMessage(String messageId)
    {
        this(messageId, null);
        
        MessageType = EMessageType.Acknowledge;
    }

    public ReliableMessage(String messageId, Object message)
    {
        MessageType = EMessageType.Message;
        MessageId = messageId;
        Message = message;
    }

    public EMessageType MessageType;

    public String MessageId;

    public Object Message;
    
    
    private static final long serialVersionUID = -4938968637220909699L;
}
