/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;

/**
 * The interface declares the composite duplex output channel.
 * The composite duplex output channel is the duplex output channel that uses underlying duplex output channel
 * to send messages and receive response messages. 
 *
 */
public interface ICompositeDuplexOutputChannel extends IDuplexOutputChannel
{
    /**
     * Returns the underlying duplex output channel.
     * @return
     */
    IDuplexOutputChannel getUnderlyingDuplexOutputChannel();
}
