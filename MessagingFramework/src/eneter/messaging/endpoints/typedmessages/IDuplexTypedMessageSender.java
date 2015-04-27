/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;

/**
 * Sender for one specified message type.
 * 
 * This is a client component which send request messages and receive response messages.
 * It can send messages to DuplextTypedMessageReceiver.
 *
 * @param <TResponse> Type of the response message which can be received.
 * @param <TRequest> Type of the request message which can be sent.
 */
public interface IDuplexTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
{
    /**
     * Raised when the connection with the receiver is open.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Raised when the service closed the connection with the client.
     * The event is raised only if the service closes the connection with the client.
     * It is not raised if the client closed the connection by IDuplexOutputChannel.closeConnection().
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * Raised when a response message is received.
     * @return
     */
    Event<TypedResponseReceivedEventArgs<TResponse>> responseReceived();
    
    /**
     * Sends message to the service.
     * @param message
     * @throws Exception 
     */
    void sendRequestMessage(TRequest message) throws Exception;
}
