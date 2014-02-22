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
 * Receiver of typed messages.
 *
 * @param <TResponse> sends response messages of this type.
 * @param <TRequest> receives messages of this type.
 */
public interface IDuplexTypedMessageReceiver<TResponse, TRequest> extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the message from a duplex typed message sender was received.
     * @return
     */
    Event<TypedRequestReceivedEventArgs<TRequest>> messageReceived();
    
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
    void sendResponseMessage(String responseReceiverId, TResponse responseMessage) throws Exception;
}
