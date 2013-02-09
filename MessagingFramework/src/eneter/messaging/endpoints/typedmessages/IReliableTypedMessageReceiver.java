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
 * The interface declares the strongly typed reliable message receiver.
 * The receiver is able to receive messages of the specified type and send back response messages of the specified type.
 * In addition it provides events notifying whether the respone message was delivered.
 * The reliable typed message receiver can be used only with the reliable typed message sender.
 *
 * @param <_ResponseType> type of the response message
 * @param <_RequestType> type of the message
 */
public interface IReliableTypedMessageReceiver<_ResponseType, _RequestType> extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the message is received.
     * @return
     */
    Event<TypedRequestReceivedEventArgs<_RequestType>> messageReceived();

    /**
     * The event is invoked when the reliable typed message sender opened connection.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when the reliable typed message sender was disconnected.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * The event is invoked when the response message was delivered.
     * @return
     */
    Event<ReliableMessageIdEventArgs> responseMessageDelivered();

    /**
     * The event is invoked when the response message was not delivered within specified time.
     * @return
     */
    Event<ReliableMessageIdEventArgs> responseMessageNotDelivered();

    /**
     * Sends the typed response message.
     * 
     * @param responseReceiverId identifies the response receiver
     * @param responseMessage respone message
     * @return id od the sent response message
     * 
     * @throws Exception
     */
    String sendResponseMessage(String responseReceiverId, _ResponseType responseMessage) throws Exception;
}
