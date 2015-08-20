/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.net.system.Event;

/**
 * Message bus.
 * 
 * The message bus is the component which can expose services.
 * The service connects the message bus and registers its service id.
 * When a client needs to use the service it connects the message bus and specifies the service id.
 * If the service id exists the message bus establishes the connection between the client and the service.<br/>
 * <br/>
 * The presence of the message bus is transparent for logic of services and their clients. The whole communication
 * is realized via {@link MessageBusMessagingFactory} which ensures the interaction with the message bus.
 *  
 */
public interface IMessageBus
{
    /**
     * The event is raised when a new service is registered. 
     * @return event
     */
    Event<MessageBusServiceEventArgs> serviceRegistered();
    
    /**
     * The event is raised when a service is unregistered.
     * @return event
     */
    Event<MessageBusServiceEventArgs> serviceUnregistered();
    
    /**
     * The event is raised when a client is connected to the service.
     * @return event
     */
    Event<MessageBusClientEventArgs> clientConnected();
    
    /**
     * The event is raised when a client is disconnected from the service.
     * @return event
     */
    Event<MessageBusClientEventArgs> clientDisconnected();
    
    /**
     * The event is raised when a client sent a message to the service.
     * @return event
     */
    Event<MessageBusMessageEventArgs> messageToServiceSent();

    /**
     * The event is raised when a service sent a message to the client.
     * @return event
     */
    Event<MessageBusMessageEventArgs> messageToClientSent();
    
    
    /**
     * Attaches input channels which are used for the communication with the message bus.
     * 
     * Once input channels are attached the message bus is listening and can be contacted by services and
     * clients.
     * 
     * @param serviceInputChannel input channel used by services.
     * @param clientInputChannel input channel used by clients.
     * @throws Exception
     */
    void attachDuplexInputChannels(IDuplexInputChannel serviceInputChannel, IDuplexInputChannel clientInputChannel) throws Exception;

    /**
     * Detaches input channels and stops the listening.
     */
    void detachDuplexInputChannels();
    
    /**
     * Returns list of all connected services.
     * @return ids of all services which are registered in the message bus.
     */
    String[] getConnectedServices();
    
    /**
     * Returns list of all clients connected to the specified service.
     * @param serviceAddress id of the service
     * @return response receiver ids of connected clients
     */
    String[] getConnectedClients(String serviceAddress);
    
    /**
     * Returns number of clients connected to the specified service.
     * Using this method is faster than GetConnectedClients because it does not have to copy data.
     * @param serviceAddress id of the service
     * @return number of connected clients
     */
    int GetNumberOfConnectedClients(String serviceAddress);

    /**
     * Disconnect and unregisters the specified service.
     * @param serviceAddress id of the service that shall be unregistered
     */
    void disconnectService(String serviceAddress);
}
