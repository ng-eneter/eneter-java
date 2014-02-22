/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Event argument used when a typed response message is received.
 *
 * @param <TResponseMessage> message type
 */
public final class TypedResponseReceivedEventArgs<TResponseMessage>
{
    /**
     * Constructs the event.
     * @param responseMessage response message
     */
    public TypedResponseReceivedEventArgs(TResponseMessage responseMessage)
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
    public TResponseMessage getResponseMessage()
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
    
    private TResponseMessage myResponseMessage;
    private Exception myReceivingError;
}
