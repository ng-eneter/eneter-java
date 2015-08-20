/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

/**
 * Event arguments used by the message bus when a service is connected / disconnected.
 *
 */
public class MessageBusServiceEventArgs
{
    /**
     * Constructs the event arguments.
     * @param serviceAdddress service id.
     */
    public MessageBusServiceEventArgs(String serviceAdddress, String responseReceiverId)
    {
        myServiceAddress = serviceAdddress;
        myResponseReceiverId = responseReceiverId;
    }

    /**
     * Returns service id.
     * @return
     */
    public String getServiceAddress()
    {
        return myServiceAddress;
    }
    
    /**
     * Returns response receiver id of the service.
     * @return
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    private String myServiceAddress;
    private String myResponseReceiverId;
}
