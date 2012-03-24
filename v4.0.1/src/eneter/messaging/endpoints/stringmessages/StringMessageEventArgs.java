/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * The string message received event.
 *
 */
public final class StringMessageEventArgs
{
    /**
     * Constructs the event.
     * @param message
     */
    public StringMessageEventArgs(String message)
    {
        myMessage = message;
    }
    
    /**
     * Returns the received string message.
     * @return
     */
    public String getMessage()
    {
        return myMessage;
    }
    
    private String myMessage; 
}
