/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.net.system.Event;

/**
 * The interface declares the strongly typed reliable message sender.
 * The reliable sender can send typed messages and receive typed response messages.
 * In addition it provides events notifying whether the message was delivered.
 * The reliable typed message sender can be used only with the reliable typed message receiver.
 *
 * @param <_ResponseType> type of the response message
 * @param <_RequestType> type of the message
 */
public interface IReliableTypedMessageSender<_ResponseType, _RequestType> extends IAttachableDuplexOutputChannel
{
    /**
     * The event is invoked when the response message is received.
     * @return
     */
    Event<TypedResponseReceivedEventArgs<_ResponseType>> responseReceived();
    
    /**
     * The event is invoked when the message was delivered.
     * @return
     */
    Event<ReliableMessageIdEventArgs> messageDelivered();
    
    /**
     * The event is invoked if the event is not delivered within a specified time.
     * @return
     */
    Event<ReliableMessageIdEventArgs> messageNotDelivered();
    
    /**
     * Sends the message to the reliable typed message receiver.
     * 
     * @param message message to be sent
     * @return id of the message. The id can be then used to check if the message was received.
     * 
     * @throws Exception
     */
    String sendRequestMessage(_RequestType message) throws Exception;
}
