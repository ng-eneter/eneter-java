/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;

/**
 * Receiver for multiple message types.
 * 
 * This is a service component which can receive request messages and send response messages.
 * In comparition with DuplexTypedMessageReceiver it can receive and send multiple types of messages. 
 *
 */
public interface IMultiTypedMessageReceiver extends IAttachableDuplexInputChannel
{
    /**
     * Raised when a new client is connected.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * Raised when a client closed the connection.
     * The event is raised only if the connection was closed by the client.
     * It is not raised if the client was disconnected by IDuplexInputChannel.disconnectResponseReceiver(...). 
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Registers message handler for specified message type.
     * If the specified message type is received the handler will be called to process it.
     *  
     * @param handler message handler which shall be called when the specified message type is received.
     * @param clazz type of the message.
     * @throws Exception
     */
    <T> void registerRequestMessageReceiver(EventHandler<TypedRequestReceivedEventArgs<T>> handler, Class<T> clazz) throws Exception;
    
    /**
     * Unregisters the message handler for the specified message type.
     * @param clazz type of the message.
     */
    <T> void unregisterRequestMessageReceiver(Class<T> clazz);
    
    /**
     * Returns the list of registered message types which can be received. 
     * @return
     */
    ArrayList<Class<?>> getRegisteredRequestMessageTypes();
    
    /**
     * Sends response message.
     * 
     * 
     * @param responseReceiverId identifies the client
     * @param responseMessage response message
     * @param clazz type of the response message
     * @throws Exception
     */
    <TResponseMessage> void sendResponseMessage(String responseReceiverId, TResponseMessage responseMessage, Class<TResponseMessage> clazz) throws Exception;
}
