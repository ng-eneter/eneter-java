/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.net.system.Event;

/**
 * The interface declares the strongly typed duplex message sender.
 * The duplex sender is able to send messages of the specified type and receive responses of the specified type.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_ResponseType> The type of receiving response messages.
 * @param <_RequestType> The type of sending messages.
 */
public interface IDuplexTypedMessageSender<_ResponseType, _RequestType> extends IAttachableDuplexOutputChannel
{
    /**
     * The event is invoked when a response message was received.
     * @return
     */
    Event<TypedResponseReceivedEventArgs<_ResponseType>> responseReceived();
    
    /**
     * Sends the strongly typed message.
     * @param message
     * @throws Exception 
     */
    void sendRequestMessage(_RequestType message) throws Exception;
}