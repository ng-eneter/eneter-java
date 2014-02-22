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
 * Reliable typed message sender (it confirms whether the message was received).
 * 
 * Declares reliable message sender which can send messages of specified type and receive response messages of specified type.
 * Reliable means it provides events notifying whether the message was delivered.
 * The reliable typed message sender can be used only with the reliable typed message receiver.
 *
 * @param <TResponse> type of the response message
 * @param <TRequest> type of the message
 */
public interface IReliableTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
{
    /**
     * The event is raised when the connection with receiver is opened.
     * @return event that can be subscribed
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * The event is raised when the connection with receiver is closed.
     * @return event that can be subscribed
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * The event is invoked when the response message is received.
     * @return event that can be subscribed
     */
    Event<TypedResponseReceivedEventArgs<TResponse>> responseReceived();
    
    /**
     * The event is invoked when the message was delivered.
     * @return event that can be subscribed
     */
    Event<ReliableMessageIdEventArgs> messageDelivered();
    
    /**
     * The event is invoked if the event is not delivered within a specified time.
     * @return event that can be subscribed
     */
    Event<ReliableMessageIdEventArgs> messageNotDelivered();
    
    /**
     * Sends the message of specified type.
     * 
     * @param message message to be sent
     * @return id of the message. The id can be then used to check if the message was received.
     * 
     * @throws Exception
     */
    String sendRequestMessage(TRequest message) throws Exception;
}
