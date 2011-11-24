package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

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
    public IChannelWrapper createChannelWrapper()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ChannelWrapper(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IChannelUnwrapper createChannelUnwrapper(
            IMessagingSystemFactory outputMessagingSystem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ChannelUnwrapper(outputMessagingSystem, mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private ISerializer mySerializer;
}
