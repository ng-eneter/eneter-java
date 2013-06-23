/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.infrastructure.attachable.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.Event;

/**
 * Declares the duplex channel unwrapper.
 * The duplex channel wrapper is listening to more duplex input channels. When it receives some message,
 * it wraps the message and sends it via the only duplex output channel.
 * On the other side the message is received by duplex channel unwrapper. The unwrapper unwraps the message
 * and uses the duplex output channel to forward the message to the correct receiver.<br/>
 * The receiver can also send the response message. Then it goes the same way back.<br/>
 * Notice, the 'duplex channel unwrapper' can communication only with 'duplex channel wrapper'.
 * It cannot communicate with one-way 'channel wrapper'.
 * 
 *
 */
public interface IDuplexChannelUnwrapper extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the duplex channel wrapper opened the connection with this unwrapper via its duplex output channel.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * The event is invoked when the duplex channel wrapper closed the connection with this unwrapper via its duplex output channel.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Returns response receiver id of the client connected to the unwrapper.
     * @param responseReceiverId responseRecieverId from unwrapped message
     * @return responseReceiverId of the client connected to the channel unwrapper. Returns null if it does not exist.
     * @throws Exception 
     */
    String getAssociatedResponseReceiverId(String responseReceiverId) throws Exception;
}
