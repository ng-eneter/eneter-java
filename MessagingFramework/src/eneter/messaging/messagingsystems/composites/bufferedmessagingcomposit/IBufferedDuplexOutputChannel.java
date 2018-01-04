/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2018 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.Event;

public interface IBufferedDuplexOutputChannel extends IDuplexOutputChannel
{
    /**
     * The event is raised when the connection gets into the online state.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOnline();
    
    /**
     * The event is raised when the connection gets into the offline state.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOffline();
    
    /**
     * Returns true if the connection is in the online state.
     * @return
     */
    boolean isOnline();
}
