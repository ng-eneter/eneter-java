package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;

public class ReliableTypedMessagesFactory implements IReliableTypedMessagesFactory
{
    public ReliableTypedMessagesFactory()
    {
        this(12000, new XmlStringSerializer());
    }
    
    public ReliableTypedMessagesFactory(int acknowledgeTimeout)
    {
        this(acknowledgeTimeout, new XmlStringSerializer());
    }
    
    public ReliableTypedMessagesFactory(int acknowledgeTimeout, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAcknowledgeTimeout = acknowledgeTimeout;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public <_ResponseType, _RequestType> IReliableTypedMessageSender<_ResponseType, _RequestType> createReliableDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ReliableDuplexTypedMessageSender<_ResponseType, _RequestType>(myAcknowledgeTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <_ResponseType, _RequestType> IReliableTypedMessageReceiver<_ResponseType, _RequestType> createReliableDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ReliableDuplexTypedMessageReceiver<_ResponseType, _RequestType>(myAcknowledgeTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private int myAcknowledgeTimeout;
    private ISerializer mySerializer;
}
