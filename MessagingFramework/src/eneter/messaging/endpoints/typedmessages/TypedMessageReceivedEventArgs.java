/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;


/**
 * The typed message received event.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_MessageData> represents the message type
 */
public final class TypedMessageReceivedEventArgs<_MessageData>
{
    /**
     * Constructs the event.
     * @param messageData message
     */
    public TypedMessageReceivedEventArgs(_MessageData messageData)
    {
        myMessageData = messageData;
        myReceivingError = null;
    }
    
    /**
     * Constructs the event from the given error message.
     * @param error error detected during receiving of the message
     */
    public TypedMessageReceivedEventArgs(Exception error)
    {
        myMessageData = null;
        myReceivingError = error;
    }
    
    /**
     * Returns the received message.
     * @return
     */
    public _MessageData getMessageData()
    {
        return myMessageData;
    }
    
    /**
     * Returns the error detected during receiving of the message.
     * E.g. during the deserialization of the message.
     * @return
     */
    public Exception getReceivingError()
    {
        return myReceivingError;
    }

    private _MessageData myMessageData;
    private Exception myReceivingError;
}
