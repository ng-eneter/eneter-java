/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;

/**
 * Synchronized sender for multiple message types (it waits until the response is received).
 * 
 * Message sender which sends request messages of specified type and receive response messages of specified type.
 * Synchronous means when the message is sent it waits until the response message is received.
 * If the waiting for the response message exceeds the specified timeout the TimeoutException is thrown.
 *
 */
public interface ISyncMultitypedMessageSender extends IAttachableDuplexOutputChannel
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
     * Sends request message and returns the response.
     * 
     * @param message request message.
     * @param responseClazz type of the expected response message.
     * @param requestClazz type of the request message.
     * @return response message.
     * @throws Exception
     */
    <TRequest, TResponse> TResponse sendRequestMessage(TRequest message, Class<TResponse> responseClazz, Class<TRequest> requestClazz) throws Exception;
}
