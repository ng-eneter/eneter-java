/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Represents messaging providing output and input channels for the communication.
 * 
 * This factory interface is supposed to be implemented by all messaging systems.
 * Particular messaging systems provides implementations for output and input channels
 * using their transportation mechanisms. E.g. for TCP, Websockets, ... . 
 * 
 */
public interface IMessagingSystemFactory
{
    /**
     * Creates the output channel which can sends and receive messages from the input channel.
     * 
     * @param channelId address of the input channel.
     * @return output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Creates the output channel which can sends and receive messages from the input channel.
     * 
     * @param channelId address of the input channel.
     * @param responseReceiverId unique identifier of the output channel.
     * @return duplex output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId, String responseReceiverId) throws Exception;
    
    /**
     * Creates the input channel which can receive and send messages to the output channel.
     * 
     * @param channelId address of the input channel.
     * @return input channel
     */
    IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception;
}