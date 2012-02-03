/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * The event when a typed response message is received.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_ResponseMessageType> message type
 */
public final class TypedResponseReceivedEventArgs<_ResponseMessageType>
{
    /**
     * Constructs the event.
     * @param responseMessage response message
     */
    public TypedResponseReceivedEventArgs(_ResponseMessageType responseMessage)
    {
        myResponseMessage = responseMessage;
        myReceivingError = null;
    }
    
    /**
     * Constructs the event from the exception detected during receiving the response message.
     * @param error
     */
    public TypedResponseReceivedEventArgs(Exception error)
    {
        myResponseMessage = null;
        myReceivingError = error;
    }
    
    /**
     * Returns the message.
     * @return
     */
    public _ResponseMessageType getResponseMessage()
    {
        return myResponseMessage;
    }
    
    /**
     * Returns an exception detected during receiving the response message.
     * @return
     */
    public Exception getReceivingError()
    {
        return myReceivingError;
    }
    
    private _ResponseMessageType myResponseMessage;
    private Exception myReceivingError;
}
