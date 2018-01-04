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

/**
 * Duplex input channel which can work offline.
 *
 */
public interface IBufferedDuplexInputChannel extends IDuplexInputChannel
{
    /**
     * The event is raised when a response receiver gets into the online state.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverOnline();
    
    /**
     * The event is raised when a response receiver gets into the offline state.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverOffline();
}
