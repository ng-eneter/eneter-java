/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;


/**
 * Implements the factory to create duplex strongly typed message sender and receiver.
 * @author Ondrej Uzovic & Martin Valach
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
    public DuplexTypedMessagesFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
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
}
