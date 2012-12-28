/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * The event when the typed message is received.
 *
 * @param <_RequestMessageType> type of the request message
 */
public final class TypedRequestReceivedEventArgs<_RequestMessageType>
{
    /**
     * Constructs the event.
     * @param responseReceiverId identifies the client where the response can be sent
     * @param senderAddress address of the message sender. It is null if not applicable for the messaging system.
     * @param requestMessage message
     */
    public TypedRequestReceivedEventArgs(String responseReceiverId, String senderAddress, _RequestMessageType requestMessage)
    {
        myRequestMessage = requestMessage;
        myResponseReceiverId = responseReceiverId;
        mySenderAddress = senderAddress;
        myReceivingError = null;
    }
    
    /**
     * Constructs the message from the exception.
     * @param responseReceiverId identifies the client where the response can be sent
     * @param senderAddress address of the message sender. It is null if not applicable for the messaging system.
     * @param error error detected during receiving the message
     */
    public TypedRequestReceivedEventArgs(String responseReceiverId, String senderAddress, Exception error)
    {
        myRequestMessage = null;
        myResponseReceiverId = responseReceiverId;
        mySenderAddress = senderAddress;
        myReceivingError = error;
    }

    /**
     * Returns the received message.
     * @return
     */
    public _RequestMessageType getRequestMessage()
    {
        return myRequestMessage;
    }
    
    /**
     * Returns the client identifier where the response can be sent.
     * @return
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    /**
     * Returns the address where the sender of the request message is located. (e.g. IP address of the client).
     * It can be empty string if not applicable for used messaging.
     * @return
     */
    public String getSenderAddress()
    {
        return mySenderAddress;
    }
    
    /**
     * Returns the error detected during receiving of the message.
     * @return
     */
    public Exception getReceivingError()
    {
        return myReceivingError;
    }
    
    private _RequestMessageType myRequestMessage;
    private String myResponseReceiverId;
    private String mySenderAddress;
    private Exception myReceivingError;
}
