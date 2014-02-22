/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.connectionprotocols.internal.LocalProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;

/**
 * Messaging system delivering messages synchronously (like a synchronous local call).
 * It creates output and input channels using the caller thread to deliver messages.
 * <br/><br/>
 * Different instances of SynchronousMessagingSystemFactory are independent and so they
 * are different messaging systems. Therefore if you want to send/receive a message through this messaging system
 * then output and input channels must be created with the same instance of SynchronousMessagingSystemFactory.
 * 
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
        this(new LocalProtocolFormatter());
    }
    
    /**
     * Constructs the factory representing the messaging system.
     * 
     * @param protocolFormatter formatter used to encode low-level messages between channels
     */
    public SynchronousMessagingSystemFactory(IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDefaultMessagingFactory = new DefaultMessagingSystemFactory(new SynchronousMessagingProvider(), protocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex output channel communicating with the specified duplex input channel using synchronous local call.
     * The duplex output channel can send messages and receive response messages. 
     * @throws Exception 
     */
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDefaultMessagingFactory.createDuplexOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel communicating with the specified duplex input channel using synchronous local call.
     * The duplex output channel can send messages and receive response messages.
     * @throws Exception 
     */
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId, String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDefaultMessagingFactory.createDuplexOutputChannel(channelId, responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel listening to messages on the specified channel id.
     * The duplex input channel can send response messages back to the duplex output channel.
     * @throws Exception 
     */
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDefaultMessagingFactory.createDuplexInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return
     */
    public SynchronousMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myDefaultMessagingFactory.setInputChannelThreading(inputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myDefaultMessagingFactory.getInputChannelThreading();
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return
     */
    public SynchronousMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myDefaultMessagingFactory.setOutputChannelThreading(outputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myDefaultMessagingFactory.getOutputChannelThreading();
    }
    
    
    
    private DefaultMessagingSystemFactory myDefaultMessagingFactory;
}
