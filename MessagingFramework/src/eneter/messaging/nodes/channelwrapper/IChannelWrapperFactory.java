/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

/**
 * Declares the factory for creating channel wrappers and and channel unwrappers.
 *
 */
public interface IChannelWrapperFactory
{
    /**
     * Creates the duplex channel wrapper.
     * @return
     */
    IDuplexChannelWrapper createDuplexChannelWrapper();
    
    /**
     * Creates the duplex channel unwrapper.
     * @param outputMessagingSystem Messaging used to create output channels where unwrapped messages will be sent.
     * @return
     */
    IDuplexChannelUnwrapper createDuplexChannelUnwrapper(IMessagingSystemFactory outputMessagingSystem);
}
