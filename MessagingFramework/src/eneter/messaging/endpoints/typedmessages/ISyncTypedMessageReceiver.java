package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

public interface ISyncTypedMessageReceiver<TResponse, TRequest> extends IAttachableDuplexInputChannel
{
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
}
