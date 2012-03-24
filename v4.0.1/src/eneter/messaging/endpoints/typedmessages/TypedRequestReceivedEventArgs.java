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
     * @param requestMessage message
     */
    public TypedRequestReceivedEventArgs(String responseReceiverId, _RequestMessageType requestMessage)
    {
        myRequestMessage = requestMessage;
        myResponseReceiverId = responseReceiverId;
        myReceivingError = null;
    }
    
    /**
     * Constructs the message from the exception.
     * @param responseReceiverId identifies the client where the response can be sent
     * @param error error detected during receiving the message
     */
    public TypedRequestReceivedEventArgs(String responseReceiverId, Exception error)
    {
        myRequestMessage = null;
        myResponseReceiverId = responseReceiverId;
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
     * Returns the error detected during receiving of the message.
     * @return
     */
    public Exception getReceivingError()
    {
        return myReceivingError;
    }
    
    private _RequestMessageType myRequestMessage;
    private String myResponseReceiverId;
    private Exception myReceivingError;
}
