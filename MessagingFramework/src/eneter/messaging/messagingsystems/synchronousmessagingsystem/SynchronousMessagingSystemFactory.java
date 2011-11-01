/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.*;

/**
 * The factory class implements the messaging system delivering messages synchronously in the caller thread.
 * It creates output and input channels using the caller thread to deliver messages.
 * <br/><br/>
 * Different instances of SynchronousMessagingSystemFactory are independent and so they
 * are different messaging systems. Therefore if you want to send/receive a message through this messaging system
 * then output and input channels must be created with the same instance of SynchronousMessagingSystemFactory.
 * 
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public class SynchronousMessagingSystemFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory representing the messaging system.
     * Note: Every instance of the synchronous messaging system factory represents one messaging system.
     *       It means that two instances of this factory class creates channels for two independent messaging system.
     */
    public SynchronousMessagingSystemFactory()
    {
        myMessagingSystem = new SimpleMessagingSystem(new SynchronousMessagingProvider());
    }
    
    /**
     * Creates the output channel sending messages to specified input channel using the synchronous local call.
     * 
     * @param channelId identifies the receiving input channel
     * @return output channel
     */
    public IOutputChannel createOutputChannel(String channelId)
    {
        return new SimpleOutputChannel(channelId, myMessagingSystem);
    }

    /**
     * Creates the input channel receiving messages on the specified channel id via the synchronous local call.
     * 
     * @param channelId identifies this input channel
     * @return input channel
     */
    public IInputChannel createInputChannel(String channelId)
    {
        return new SimpleInputChannel(channelId, myMessagingSystem);
    }

    /**
     * Creates the duplex output channel communicating with the specified duplex input channel using synchronous local call.
     * The duplex output channel can send messages and receive response messages. 
     */
    public IDuplexOutputChannel CreateDuplexOutputChannel(String channelId)
    {
        return new SimpleDuplexOutputChannel(channelId, null, this);
    }

    /**
     * Creates the duplex output channel communicating with the specified duplex input channel using synchronous local call.
     * The duplex output channel can send messages and receive response messages.
     */
    public IDuplexOutputChannel CreateDuplexOutputChannel(String channelId, String responseReceiverId)
    {
        return new SimpleDuplexOutputChannel(channelId, responseReceiverId, this);
    }

    /**
     * Creates the duplex input channel listening to messages on the specified channel id.
     * The duplex input channel can send response messages back to the duplex output channel.
     */
    public IDuplexInputChannel CreateDuplexInputChannel(String channelId)
    {
        return new SimpleDuplexInputChannel(channelId, this);
    }
    
    
    private IMessagingSystemBase myMessagingSystem;
}
