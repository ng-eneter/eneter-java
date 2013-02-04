/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the factory to create strongly typed message senders and receivers.
 *
 */
public class TypedMessagesFactory implements ITypedMessagesFactory
{
    /**
     * Constructs the typed messages factory with BinarySerializer.<br/>
     * <b>Note: The serializer is XmlStringSerializer in case of Silverlight.</b>
     */
    public TypedMessagesFactory()
    {
        this(new XmlStringSerializer());
    }
    
    /**
     * Constructs the typed message factory with specified serializer.
     * @param serializer serializer
     */
    public TypedMessagesFactory(int syncResponseReceiveTimeout)
    {
        this(new XmlStringSerializer());
    }
    
    public TypedMessagesFactory(ISerializer serializer)
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
     * Creates the typed message sender.
     * The sender sends the messages via attached one-way output channel.
     */
    @Override
    public <_MessageDataType> ITypedMessageSender<_MessageDataType> createTypedMessageSender(Class<_MessageDataType> messageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new TypedMessageSender<_MessageDataType>(mySerializer, messageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the typed message receiver.
     * The receiver receives messages via attached one-way input channel.
     */
    @Override
    public <_MessageDataType> ITypedMessageReceiver<_MessageDataType> createTypedMessageReceiver(Class<_MessageDataType> messageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new TypedMessageReceiver<_MessageDataType>(mySerializer, messageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer mySerializer;
}
