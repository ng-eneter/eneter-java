/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The interface declares the factory that creates input and output channels.
 * This factory interface is supposed to be implemented by particular messaging systems so that
 * a custom implementation for input and output channels can be provided.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public interface IMessagingSystemFactory
{
	/**
	 * Creates the output channel sending messages to specified input channel.
	 * 
	 * @param channelId identifies the receiving input channel
	 * @return output channel
	 * @throws Exception 
	 */
    IOutputChannel createOutputChannel(String channelId) throws Exception;

    /**
     * Creates the input channel listening to messages on the specified channel id.
     * 
     * @param channelId identifies this input channel
     * @return input channel
     * @throws Exception 
     */
    IInputChannel createInputChannel(String channelId) throws Exception;
    
    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * @param channelId identifies the receiving duplex input channel
     * @return duplex output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * @param channelId identifies the receiving duplex input channel
     * @param responseReceiverId unique identifier of the response receiver represented by this duplex output channel.
     * @return duplex output channel
     */
    IDuplexOutputChannel createDuplexOutputChannel(String channelId, String responseReceiverId) throws Exception;
    
    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages.
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     * @param channelId identifies the address, the duplex input channel listens to
     * @return duplex input channel
     */
    IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception;
}