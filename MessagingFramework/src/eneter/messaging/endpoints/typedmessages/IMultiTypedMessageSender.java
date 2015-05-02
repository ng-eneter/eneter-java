/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;

/**
 * Sender for multiple message types.
 * 
 * This is a client component which can send and receive messages of multiple types.<br/>
 *
 */
public interface IMultiTypedMessageSender extends IAttachableDuplexOutputChannel
{
    /**
     * Raised when the connection with the receiver is open.
     * @return event
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Raised when the service closed the connection with the client.
     * The event is raised only if the service closes the connection with the client.
     * It is not raised if the client closed the connection by IDuplexOutputChannel.closeConnection().
     * @return event
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * Registers response message handler for specified message type.
     * 
     * If the response message of the specified type is received the handler will be called to process it.
     * 
     * @param handler response message handler.
     * @param clazz type of the response message.
     * @throws Exception
     */
    <T> void registerResponseMessageReceiver(EventHandler<TypedResponseReceivedEventArgs<T>> handler, Class<T> clazz) throws Exception;
    
    /**
     * Unregisters the response message handler for the specified message type.
     * @param clazz
     */
    <T> void unregisterResponseMessageReceiver(Class<T> clazz);
    
    /**
     * Returns the list of registered response message types which can be received.
     * @return registered message types
     */
    ArrayList<Class<?>> getRegisteredResponseMessageTypes();
    
    /**
     * Sends request message.
     * 
     * @param message request message
     * @param clazz type of the message
     * @throws Exception
     */
    <TRequestMessage> void sendRequestMessage(TRequestMessage message, Class<TRequestMessage> clazz) throws Exception;
}
