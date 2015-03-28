/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;


/**
 * Implements factory for creating the message bus.
 *
 */
public class MessageBusFactory implements IMessageBusFactory
{
    /**
     * Constructs the factory with default parameters.
     * 
     * It uses internal MessageBusCustomSerializer which is optimazed to serialize/deserialze only the MessageBusMessage.
     */
    public MessageBusFactory()
    {
        this(new MessageBusCustomSerializer());
    }

    /**
     * Construct the factory.
     * @param protocolFormatter Serializer which will be used to serialize/deserialize MessageBusMessage.
     */
    public MessageBusFactory(ISerializer serializer)
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

    @Override
    public IMessageBus createMessageBus()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MessageBus(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer mySerializer;
}
