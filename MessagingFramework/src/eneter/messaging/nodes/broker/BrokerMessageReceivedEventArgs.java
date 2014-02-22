/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Event arguments of the received message from the broker.
 *
 */
public final class BrokerMessageReceivedEventArgs
{
    /**
     * Constructs the event from the input parameters.
     * @param messageTypeId
     * @param message
     */
    public BrokerMessageReceivedEventArgs(String messageTypeId, Object message)
    {
        myMessageTypeId = messageTypeId;
        myMessage = message;

        myReceivingError = null;
    }
    
    /**
     * Constructs the event from the error detected during receiving of the message.
     * @param receivingError
     */
    public BrokerMessageReceivedEventArgs(Exception receivingError)
    {
        myMessageTypeId = "";
        myMessage = "";

        myReceivingError = receivingError;
    }
    
    /**
     * Returns type of the message.
     * @return
     */
    public String getMessageTypeId()
    {
        return myMessageTypeId;
    }
    
    /**
     * Returns the message.
     * @return
     */
    public Object getMessage()
    {
        return myMessage;
    }
    
    /**
     * Returns the error detected during receiving of the message.
     * @return
     */
    public Exception getReceivingError()
    {
        return myReceivingError;
    }

    
    private String myMessageTypeId;
    private Object myMessage;
    private Exception myReceivingError;
}
