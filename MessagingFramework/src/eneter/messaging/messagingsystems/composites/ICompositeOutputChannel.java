/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

/**
 * The interface declares the composit output channel.
 * The composit output channel is the output channel that uses underlying output channel
 * to send messages. 
 *
 */
public interface ICompositeOutputChannel extends IOutputChannel
{
    /**
     * Returns the underlying output channel.
     * @return
     */
    IOutputChannel getUnderlyingOutputChannel();
}
