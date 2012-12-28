package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;

public interface ISyncTypedMessageSender<TResponse, TRequest> extends IAttachableDuplexOutputChannel
{
    TResponse SendRequestMessage(TRequest message) throws Exception;
}
