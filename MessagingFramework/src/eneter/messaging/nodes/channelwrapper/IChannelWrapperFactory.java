package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

public interface IChannelWrapperFactory
{
    IChannelWrapper createChannelWrapper();
    
    IChannelUnwrapper createChannelUnwrapper(IMessagingSystemFactory outputMessagingSystem);
}
