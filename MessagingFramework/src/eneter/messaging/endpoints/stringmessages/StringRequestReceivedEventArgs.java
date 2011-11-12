/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * Declares the event type when the request message is received.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public final class StringRequestReceivedEventArgs
{
    /**
     * Constructs the event from thr parameters.
     * @param requestMessage
     * @param responseReceiverId
     */
    public StringRequestReceivedEventArgs(String requestMessage, String responseReceiverId)
    {
        myRequestMessage = requestMessage;
        myResponseReceiverId = responseReceiverId;
    }
    
    /**
     * Returns the request message.
     * @return
     */
    public String getRequestMessage()
    {
        return myRequestMessage;
    }
    
    /**
     * Returns the response receiver id.
     * @return
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    private String myRequestMessage;
    private  String myResponseReceiverId;
}
