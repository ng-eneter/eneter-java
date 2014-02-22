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
 * Sender of typed messages.
 *
 * @param <TResponse> receives response messages of this type.
 * @param <TRequest> sends messages of this type.
 */
public interface IDuplexTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
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
     * The event is invoked when a response message was received.
     * @return
     */
    Event<TypedResponseReceivedEventArgs<TResponse>> responseReceived();
    
    /**
     * Sends message of specified type.
     * @param message
     * @throws Exception 
     */
    void sendRequestMessage(TRequest message) throws Exception;
}
