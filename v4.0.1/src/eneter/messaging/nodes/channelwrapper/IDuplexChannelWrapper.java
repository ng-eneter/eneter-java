/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.infrastructure.attachable.*;

/**
 * Declares the duplex channel wrapper.
 * The duplex channel wrapper is listening to more duplex input channels. When it receives some message,
 * it wraps the message and sends it via the only duplex output channel.
 * On the other side the message is received by duplex channel unwrapper. The unwrapper unwraps the message
 * and uses the duplex output channel to forward the message to the correct receiver.<br/>
 * The receiver can also send the response message. Then it goes the same way back.<br/>
 * Notice, the 'duplex channel wrapper' can communication only with 'duplex channel unwrapper'.
 * It cannot communicate with one-way 'channel unwrapper'.
 * 
 *
 */
public interface IDuplexChannelWrapper extends IAttachableMultipleDuplexInputChannels,
                                               IAttachableDuplexOutputChannel
{

}
