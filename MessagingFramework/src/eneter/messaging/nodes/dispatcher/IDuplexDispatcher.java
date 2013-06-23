/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import eneter.messaging.infrastructure.attachable.IAttachableMultipleDuplexInputChannels;

/**
 * Declares the duplex dispatcher.
 * 
 * The duplex dispatcher has attached more duplex input channels and uses more duplex output channels.<br/>
 * When it receives some message via the duplex input channel it forwards the message to all duplex output channels.<br/>
 * The duplex dispatcher allows the bidirectional communication. It means, receivers to whom the message was forwarded can
 * sand back response messages. Therefore, the sender can get response messages from all receivers.
 *
 */
public interface IDuplexDispatcher extends IAttachableMultipleDuplexInputChannels
{
    /**
     * Adds the duplex output channel id to the dispatcher. The dispatcher will then start to forward
     * the incoming messages also to this channel.
     * @param channelId
     */
    void addDuplexOutputChannel(String channelId);
    
    /**
     * Removes the duplex output channel from the dispatcher.
     * @param channelId
     * @throws Exception
     */
    void removeDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Removes all duplex output channels from the dispatcher.
     * @throws Exception
     */
    void removeAllDuplexOutputChannels() throws Exception;
    
    /**
     * Returns response receiver id of the client connected to the dispatcher.
     * @param responseReceiverId responseRecieverId after dispatching
     * @return responseReceiverId of the client connected to the dispatcher. Returns null if it does not exist.
     * @throws Exception
     */
    String getAssociatedResponseReceiverId(String responseReceiverId) throws Exception;
}
