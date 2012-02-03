/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;


/**
 * The event data representing the response receiver id.
 * The event is used for the communication between the duplex output channel and duplex input channel
 * to identify where to send response messages.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public final class ResponseReceiverEventArgs
{
    /**
     * Constructs the event from the input parameters.
     * @param responseReceiverId identifies the response message receiver
     */
    public ResponseReceiverEventArgs(String responseReceiverId)
    {
        myResponseReceiverId = responseReceiverId;
    }
    
    /**
     * Returns response message receiver.
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    private String myResponseReceiverId;
}
