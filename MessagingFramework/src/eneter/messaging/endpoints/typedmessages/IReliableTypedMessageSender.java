package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.net.system.Event;

public interface IReliableTypedMessageSender<_ResponseType, _RequestType> extends IAttachableDuplexOutputChannel
{
    Event<TypedResponseReceivedEventArgs<_ResponseType>> responseReceived();
    
    Event<ReliableMessageIdEventArgs> messageDelivered();
    
    Event<ReliableMessageIdEventArgs> messageNotDelivered();
    
    String sendRequestMessage(_RequestType message) throws Exception;
}
