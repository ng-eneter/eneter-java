/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;


/**
 * Message sender that sends a request message and then waits until the response is received.
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
     * @param message request message
     * @return response message
     * @throws Exception
     */
    TResponse sendRequestMessage(TRequest message) throws Exception;
}
