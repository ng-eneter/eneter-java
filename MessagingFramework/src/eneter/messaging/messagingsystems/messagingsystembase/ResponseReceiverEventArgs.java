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
 */
public final class ResponseReceiverEventArgs
{
    /**
     * Constructs the event from the input parameters.
     * @param responseReceiverId Unique logical id identifying the receiver of response messages.
     * @param senderAddress Address where the sender of the request message is located. (e.g. IP address of the client)<br/>
     * Can be empty string if not applicable in used messaging.
     */
    public ResponseReceiverEventArgs(String responseReceiverId, String senderAddress)
    {
        myResponseReceiverId = responseReceiverId;
        mySenderAddress = senderAddress;
    }
    
    /**
     * Returns the unique logical id identifying the receiver of response messages.
     * This id identifies who receives the response message on the client side.
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    /**
     * Returns the address where the sender of the message is located. (e.g. IP address of the client).
     * It can be empty string if not applicable for used messaging.
     * @return
     */
    public String getSenderAddress()
    {
        return mySenderAddress;
    }
    
    private String myResponseReceiverId;
    private String mySenderAddress;
}
