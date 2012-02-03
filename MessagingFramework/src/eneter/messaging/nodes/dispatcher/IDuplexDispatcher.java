/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import eneter.messaging.infrastructure.attachable.IAttachableMultipleDuplexInputChannels;

public interface IDuplexDispatcher extends IAttachableMultipleDuplexInputChannels
{
    void addDuplexOutputChannel(String channelId);
    
    void removeDuplexOutputChannel(String channelId) throws Exception;
    
    void removeAllDuplexOutputChannels() throws Exception;
}
