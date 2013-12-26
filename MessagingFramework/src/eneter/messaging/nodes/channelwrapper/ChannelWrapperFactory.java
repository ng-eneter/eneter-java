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
 * Implements the factory for creating channel wrapper and unwrapper.
 *
 */
public class ChannelWrapperFactory implements IChannelWrapperFactory
{
    public ChannelWrapperFactory()
    {
        this(new XmlStringSerializer());
    }

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
    
    private ISerializer mySerializer;
}
