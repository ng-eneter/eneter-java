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
 * The message bus is the component that allows to dynamically expose various services.
 * Services that want to be exposed via the message bus connect the message bus and register their service ids.
 * Then, if a client wants to use the service it connects the message bus and asks for the particular service id.
 * If such service id exists the message bus mediates the communication between the client and the service.<br/>
 * <br/>
 * The presence of the message bus is transparent for logic of services and their clients. The whole communication
 * is realized via {@link MessageBusMessagingFactory} which ensures the interaction with the message bus.
 *  
 */
public interface IMessageBus
{
    /**
     * The event is raised when a new service is registered. 
     * @return
     */
    Event<MessageBusServiceEventArgs> serviceRegistered();
    
    /**
     * The event is raised when a service is unregistered.
     * @return
     */
    Event<MessageBusServiceEventArgs> serviceUnregistered();
    
    /**
     * Attaches duplex input channels that are used by clients and services to connect the message bus.
     * 
     * Once input channels are attached the message bus is listening and can be contacted by services and
     * clients. <br/>
     * <br/>
     * To connect the message bus services must use 'Message Bus Duplex Input Channel' and clients must use
     * 'Message Bus Duplex Output Channel'. 
     * 
     * @param serviceInputChannel input channel used by services to register in the message bus.
     * @param clientInputChannel input channel used by clients to connect a service via the message bus.
     * @throws Exception
     */
    void attachDuplexInputChannels(IDuplexInputChannel serviceInputChannel, IDuplexInputChannel clientInputChannel) throws Exception;

    /**
     * Detaches input channels and stops the listening.
     */
    void detachDuplexInputChannels();
    
    /**
     * Returns list of all connected services.
     * @return
     */
    String[] getConnectedServices();

    /**
     * Disconnect and unregisters the specified service.
     * @param serviceAddress id of the service that shall be unregistered
     */
    void disconnectService(String serviceAddress);
}
