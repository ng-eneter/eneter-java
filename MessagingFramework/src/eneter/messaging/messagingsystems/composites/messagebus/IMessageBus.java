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
     * Disconnect and unregisters the specified service.
     * @param serviceAddress id of the service that shall be unregistered
     */
    void disconnectService(String serviceAddress);
}
