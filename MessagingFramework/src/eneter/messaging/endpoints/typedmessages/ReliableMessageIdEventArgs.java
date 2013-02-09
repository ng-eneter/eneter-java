/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

public class ReliableMessageIdEventArgs
{
    public ReliableMessageIdEventArgs(String messageId)
    {
        myMessageId = messageId;
    }
    
    public String getMessageId()
    {
        return myMessageId;
    }
    
    private String myMessageId; 
}
