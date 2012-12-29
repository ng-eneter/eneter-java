package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IFunction1;

public class SyncTypedMessagesFactory implements ISyncTypedMessagesFactory
{
    public SyncTypedMessagesFactory()
    {
        this(0, new XmlStringSerializer());
    }
    
    public SyncTypedMessagesFactory(int responseReceiveTimeout)
    {
        this(responseReceiveTimeout, new XmlStringSerializer());
    }
    
    public SyncTypedMessagesFactory(int responseReceiveTimout, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiveTimeout = responseReceiveTimout;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TResponse, TRequest> ISyncTypedMessageSender<TResponse, TRequest> createSyncMessageSender(
            Class<TResponse> responseMessageClazz,
            Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SyncTypedMessageSender<TResponse, TRequest> aSender = new SyncTypedMessageSender<TResponse, TRequest>(myResponseReceiveTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
            return aSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TResponse, TRequest> ISyncTypedMessageReceiver<TResponse, TRequest> createSyncMessageReceiver(
            IFunction1<TResponse, TypedRequestReceivedEventArgs<TRequest>> requestHandler,
            Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SyncTypedMessageReceiver<TResponse, TRequest> aReceiver = new SyncTypedMessageReceiver<TResponse, TRequest>(requestHandler, mySerializer, responseMessageClazz, requestMessageClazz);
            return aReceiver;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private int myResponseReceiveTimeout;
    private ISerializer mySerializer;
}
