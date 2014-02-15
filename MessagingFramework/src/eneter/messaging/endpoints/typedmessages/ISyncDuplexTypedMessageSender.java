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
 * Declares message sender that sends request messages of specified type and receive response messages of specified type.
 * Synchronous means it when the message is sent it waits until the response message is received.
 * If the waiting for the response message exceeds the specified timeout the {@link TimeoutException} is thrown.
 *
 * @param <TResponse> Response message type.
 * @param <TRequest> Request message type.
 */
public interface ISyncDuplexTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
{
    /**
     * The event is raised when the connection with receiver is opened.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * The event is raised when the connection with receiver is closed.
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
