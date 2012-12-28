package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

public interface IReliableTypedMessageReceiver<_ResponseType, _RequestType> extends IAttachableDuplexInputChannel
{
    Event<TypedRequestReceivedEventArgs<_RequestType>> messageReceived();

    Event<ResponseReceiverEventArgs> responseReceiverConnected();

    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();

    Event<ReliableMessageIdEventArgs> responseMessageDelivered();

    Event<ReliableMessageIdEventArgs> responseMessageNotDelivered();

    String sendResponseMessage(String responseReceiverId, _ResponseType responseMessage) throws Exception;
}
