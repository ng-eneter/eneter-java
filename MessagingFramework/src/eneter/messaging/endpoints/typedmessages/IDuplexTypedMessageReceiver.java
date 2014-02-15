/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * Declares receiver that receive messages of specified type and sends responses of specified type.
 *
 * @param <_ResponseType> sends response messages of this type.
 * @param <_RequestType> receives messages of this type.
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
