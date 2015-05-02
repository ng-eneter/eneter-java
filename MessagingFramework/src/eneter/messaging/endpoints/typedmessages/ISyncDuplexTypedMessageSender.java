/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.concurrent.TimeoutException;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;


/**
 * Synchronized sender for one specified message type (it waits until the response is received).
 * 
 * Message sender which sends request messages of specified type and receive response messages of specified type.
 * Synchronous means when the message is sent it waits until the response message is received.
 * If the waiting for the response message exceeds the specified timeout the TimeoutException is thrown.
 *
 * @param <TResponse> Response message type.
 * @param <TRequest> Request message type.
 */
public interface ISyncDuplexTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
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
     * Sends the request message and returns the response.
     * 
     * It waits until the response message is received. If waiting for the response exceeds the specified timeout
     * {@link TimeoutException} is thrown.
     * 
     * @param message request message
     * @return response message
     * @throws Exception
     */
    TResponse sendRequestMessage(TRequest message) throws Exception;
}
