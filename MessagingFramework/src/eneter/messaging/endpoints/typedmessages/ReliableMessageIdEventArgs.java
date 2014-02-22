/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Event arguments used for notification whether the message was delivered or not delivered.
 * 
 *
 */
public class ReliableMessageIdEventArgs
{
    /**
     * Constructs the event arguments.
     * @param messageId id of the message
     */
    public ReliableMessageIdEventArgs(String messageId)
    {
        myMessageId = messageId;
    }
    
    /**
     * Returns id of the message.
     * @return
     */
    public String getMessageId()
    {
        return myMessageId;
    }
    
    private String myMessageId; 
}
