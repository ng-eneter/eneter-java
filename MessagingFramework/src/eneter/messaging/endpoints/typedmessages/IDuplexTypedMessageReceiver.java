/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * Receiver for one specified message type.
 * 
 * This is a service component which can receive request messages and send back response messages.
 * It can receive messages only from DuplexTypedMessageSender or SyncDuplexTypedMessageSender.
 *
 * @param <TResponse> Type of the response message which can be sent back.
 * @param <TRequest> Type of the request message which can be received.
 */
public interface IDuplexTypedMessageReceiver<TResponse, TRequest> extends IAttachableDuplexInputChannel
{
    /**
     * Raised when a message is received.
     * @return
     */
    Event<TypedRequestReceivedEventArgs<TRequest>> messageReceived();
    
    /**
     * Raised when a new client is connected.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * Raised when a client closed the connection.
     * The event is raised only if the connection was closed by the client.
     * It is not raised if the client was disconnected by IDuplexInputChannel.disconnectResponseReceiver(...). 
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * Sends message to the client.
     * If the parameter responseReceiverId is * then it sends the broadcast message to all connected clients.
     * @param responseReceiverId identifies the client
     * @param responseMessage response message
     * @throws Exception 
     */
    void sendResponseMessage(String responseReceiverId, TResponse responseMessage) throws Exception;
}
