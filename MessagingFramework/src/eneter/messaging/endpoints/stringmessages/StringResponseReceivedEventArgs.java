/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.stringmessages;

/**
 * Event type for text response message is received.
 *
 */
public final class StringResponseReceivedEventArgs
{
    /**
     * Constructs the event.
     * @param responseMessage
     */
    public StringResponseReceivedEventArgs(String responseMessage)
    {
        myResponseMessage = responseMessage;
    }
    
    /**
     * Returns the response message.
     * @return
     */
    public String getResponseMessage()
    {
        return myResponseMessage;
    }
    
    private String myResponseMessage;
}
