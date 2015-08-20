/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2015
*/

package eneter.messaging.messagingsystems.composites.messagebus;

/**
 * Event arguments used by message when a message was transferred to a service or to a client.
 *
 */
public class MessageBusMessageEventArgs
{
    /**
     * Constructs the event arguments.
     * 
     * @param serviceAddress id of service.
     * @param serviceResponseReceiverId response receiver id of the service.
     * @param clientResponseReceiverId response receiver id of the client.
     * @param message message which is sent from client to service or from service to client.
     */
    public MessageBusMessageEventArgs(String serviceAddress, String serviceResponseReceiverId, String clientResponseReceiverId, Object message)
    {
        myServiceAddress = serviceAddress;
        myServiceResponseReceiverId = serviceResponseReceiverId;
        myClientResponseReceiverId = clientResponseReceiverId;
        myMessage = message;
    }

    /**
     * Returns service id.
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
     * @return response receiver id
     */
    public String getClientResponseReceiverId()
    {
        return myClientResponseReceiverId;
    }
    
    /**
     * Returns message which is between client and service.
     * @return message data
     */
    public Object getMessage()
    {
        return myMessage;
    }
    
    private String myServiceAddress;
    private String myServiceResponseReceiverId;
    private String myClientResponseReceiverId;
    private Object myMessage;
}
