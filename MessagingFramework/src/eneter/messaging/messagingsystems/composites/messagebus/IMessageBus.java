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
 * Declares message bus component.
 * 
 * The message bus is the component that exposes multiple services.
 * When a service wants to expose its functionality via the message bus it connects the message bus and registers there.
 * Then when a client wants to use the service it connects the message bus and asks for the service.
 * If the requested service is registered the communication between the client and the service is mediated via the message bus.
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
     * @param serviceAddress
     */
    void disconnectService(String serviceAddress);
}
