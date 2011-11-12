/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.stringmessages;

/**
 * The event is invoked when a string response message is received.
 * @author Ondrej Uzovic & Martin Valach
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
