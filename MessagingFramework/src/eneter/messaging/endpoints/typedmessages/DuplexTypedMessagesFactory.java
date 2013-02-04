/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;


/**
 * Implements the factory to create duplex strongly typed message sender and receiver.
 *
 */
public class DuplexTypedMessagesFactory implements IDuplexTypedMessagesFactory
{
    /**
     * Constructs the factory with xml serializer.
     */
    public DuplexTypedMessagesFactory()
    {
        this(new XmlStringSerializer());
    }

    /**
     * Constructs the method factory with specified serializer.
     * @param serializer serializer used to serialize/deserialize messages
     */
    public DuplexTypedMessagesFactory(int syncResponseReceiveTimeout)
    {
        this(syncResponseReceiveTimeout, new XmlStringSerializer());
    }
    
    public DuplexTypedMessagesFactory(ISerializer serializer)
    {
        this(-1, serializer);
    }
    
    public DuplexTypedMessagesFactory(int syncResponseReceiveTimeout, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            mySyncResponseReceiveTimeout = syncResponseReceiveTimeout;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates duplex typed message sender.
     */
    @Override
    public <_ResponseType, _RequestType> IDuplexTypedMessageSender<_ResponseType, _RequestType> createDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageSender<_ResponseType, _RequestType>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <_ResponseType, _RequestType> ISyncDuplexTypedMessageSender<_ResponseType, _RequestType> createSyncDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SyncTypedMessageSender<_ResponseType, _RequestType> aSender = new SyncTypedMessageSender<_ResponseType, _RequestType>(mySyncResponseReceiveTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
            return aSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex typed message receiver.
     */
    @Override
    public <_ResponseType, _RequestType> IDuplexTypedMessageReceiver<_ResponseType, _RequestType> createDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageReceiver<_ResponseType, _RequestType>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer mySerializer;
    private int mySyncResponseReceiveTimeout;
}
