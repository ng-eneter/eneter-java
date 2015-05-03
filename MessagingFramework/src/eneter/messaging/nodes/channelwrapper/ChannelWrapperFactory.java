/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

/**
 * Factory for creating channel wrapper and unwrapper.
 *
 */
public class ChannelWrapperFactory implements IChannelWrapperFactory
{
    /**
     * Constructs the factory.
     */
    public ChannelWrapperFactory()
    {
        this(new XmlStringSerializer());
    }

    /**
     * Constructs the factory.
     * @param serializer serializer used for wrapping channels with data messages.
     */
    public ChannelWrapperFactory(ISerializer serializer)
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
    public IDuplexChannelWrapper createDuplexChannelWrapper()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexChannelWrapper(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexChannelUnwrapper createDuplexChannelUnwrapper(IMessagingSystemFactory outputMessagingSystem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexChannelUnwrapper(outputMessagingSystem, mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets serializer which shall be used to serialize/deserialize DataWrapper.
     * @param serializer serializer
     * @return this ChannelWrapperFactory
     */
    public ChannelWrapperFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    /**
     * Gets serializer which is used to serialize/deserialize DataWrapper.
     * @return serializer
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    private ISerializer mySerializer;
}
