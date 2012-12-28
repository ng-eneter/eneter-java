package eneter.messaging.endpoints.typedmessages;

import eneter.net.system.IFunction1;

public interface ISyncTypedMessagesFactory
{
    <TResponse, TRequest> ISyncTypedMessageSender<TResponse, TRequest> CreateSyncMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
    
    <TResponse, TRequest> ISyncTypedMessageReceiver<TResponse, TRequest> CreateSyncMessageReceiver(IFunction1<TypedRequestReceivedEventArgs<TRequest>, TResponse> requestHandler);
}
