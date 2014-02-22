/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Messaging factory to create output and input channels.
 * 
 * This factory interface is supposed to be implemented by all messaging systems.
 * Particular messaging systems are then supposed to provide correct implementations for output and input channels
 * using their transportation mechanisms. E.g. for TCP, Websockets, ... . 
 * 
 */
public interface IMessagingSystemFactory
{
    /**
     * Creates the duplex output channel that sends messages to the duplex input channel and receives response messages.
     * 
     * @param channelId id representing receiving input channel address.
     * @return duplex output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Creates the duplex output channel that sends messages to the duplex input channel and receives response messages.
     * 
     * @param channelId id representing receiving input channel address.
     * @param responseReceiverId unique identifier of the response receiver represented by this duplex output channel.
     * @return duplex output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId, String responseReceiverId) throws Exception;
    
    /**
     * Creates the duplex input channel that receives messages from the duplex output channel and sends back response messages.
     * 
     * @param channelId id representing the input channel address.
     * @return duplex input channel
     */
    IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception;
}