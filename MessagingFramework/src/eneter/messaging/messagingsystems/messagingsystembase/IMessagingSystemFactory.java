/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Declares factory for creating communicating channels.
 * This factory interface is supposed to be implemented by particular messaging systems so that
 * a custom implementation for particular transportation mechanisms can be provided (E.g. for TCP, Web Sockets, Thread Messaging, ...)
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