/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

/**
 * Event type for text request message is received.
 *
 */
public final class StringRequestReceivedEventArgs
{
    /**
     * Constructs the event.
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
