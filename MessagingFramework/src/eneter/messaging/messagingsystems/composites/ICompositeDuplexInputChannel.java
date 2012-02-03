/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

/**
 * The interface declares the composite duplex input channel.
 * The composite duplex input channel is the duplex input channel that uses underlying duplex input channel
 * to receive messages and send responses messages.
 *
 */
public interface ICompositeDuplexInputChannel extends IDuplexInputChannel
{
    /**
     * Returns the underlying duplex input channel.
     * @return
     */
    IDuplexInputChannel getUnderlyingDuplexInputChannel();
}
