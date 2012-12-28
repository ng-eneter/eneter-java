package eneter.messaging.endpoints.typedmessages;

public interface IReliableTypedMessagesFactory
{
    <_ResponseType, _RequestType> IReliableTypedMessageSender<_ResponseType, _RequestType> createReliableDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
    
    <_ResponseType, _RequestType> IReliableTypedMessageReceiver<_ResponseType, _RequestType> createReliableDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
}
