/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * The interface declares the strongly typed duplex message receiver.
 * The receiver is able to receive messages of the specified type and send back response messages of specified type.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_ResponseType> The type of sending response messages.
 * @param <_RequestType> The type of receiving messages.
 */
public interface IDuplexTypedMessageReceiver<_ResponseType, _RequestType> extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the message from a duplex typed message sender was received.
     * @return
     */
    Event<TypedRequestReceivedEventArgs<_RequestType>> messageReceived();
    
    /**
     * The event is invoked when a duplex typed message sender opened the connection via its duplex output channel.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    /**
     * The event is invoked when a duplex typed message sender closed the connection via its duplex output channel.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    /**
     * Sends the response message back to the duplex typed message sender via the attached duplex input channel.
     * @param responseReceiverId identifies the duplex typed message sender that will receive the response
     * @param responseMessage response message
     * @throws Exception 
     */
    void sendResponseMessage(String responseReceiverId, _ResponseType responseMessage) throws Exception;
}
