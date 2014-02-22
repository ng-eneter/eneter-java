/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * Reliable typed message receiver (it confirms whether the message was received).
 * 
 * Declares the reliable message receiver that can send messages of specified type and sends back response messages of specified type.
 * Reliable means it provides events notifying whether the response message was delivered or not.
 * The reliable typed message receiver can be used only with the reliable typed message sender.
 *
 * @param <TResponse> type of the response message
 * @param <TRequest> type of the message
 */
public interface IReliableTypedMessageReceiver<TResponse, TRequest> extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the message is received.
     * @return event that can be subscribed
     */
    Event<TypedRequestReceivedEventArgs<TRequest>> messageReceived();

    /**
     * The event is invoked when the reliable typed message sender opened connection.
     * @return event that can be subscribed
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when the reliable typed message sender was disconnected.
     * @return event that can be subscribed
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * The event is invoked when the response message was delivered.
     * @return event that can be subscribed
     */
    Event<ReliableMessageIdEventArgs> responseMessageDelivered();

    /**
     * The event is invoked when the response message was not delivered within specified time.
     * @return event that can be subscribed
     */
    Event<ReliableMessageIdEventArgs> responseMessageNotDelivered();

    /**
     * Sends the response message of specified type.
     * 
     * @param responseReceiverId identifies the response receiver
     * @param responseMessage response message
     * @return id of the sent response message
     * 
     * @throws Exception
     */
    String sendResponseMessage(String responseReceiverId, TResponse responseMessage) throws Exception;
}
