/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2015
*/

package eneter.messaging.messagingsystems.composites.messagebus;


/**
 * Event arguments used by the message bus when a client is connected/disconnected.
 *
 */
public class MessageBusClientEventArgs
{
    /**
     * Constructs the event arguments.
     * 
     * @param serviceAddress id of service
     * @param serviceResponseReceiverId response receiver id of the service.
     * @param clientResponseReceiverId response receiver id of the client.
     */
    public MessageBusClientEventArgs(String serviceAddress, String serviceResponseReceiverId, String clientResponseReceiverId)
    {
        myServiceAddress = serviceAddress;
        myServiceResponseReceiverId = serviceResponseReceiverId;
        myClientResponseReceiverId = clientResponseReceiverId;
    }
    
    /**
     * Returns service id
     * @return service id
     */
    public String getServiceAddress()
    {
        return myServiceAddress;
    }
    
    /**
     * Returns response receiver id of the service.
     * @return response receiver id
     */
    public String getServiceResponseReceiverId()
    {
        return myServiceResponseReceiverId;
    }
    
    /**
     * Returns response receiver id of the client.
     * @return
     */
    public String getClientResponseReceiverId()
    {
        return myClientResponseReceiverId;
    }
    
    private String myServiceAddress;
    private String myServiceResponseReceiverId;
    private String myClientResponseReceiverId;
}
