/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.threadmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.connectionprotocols.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;


/**
 * Messaging system delivering messages to the particular working thread.
 * Each input channel is represented by its own working thread removing messages from the queue and processing them
 * one by one.
 * <br/><br/>
 * Different instances of ThreadMessagingSystemFactory are independent and so they
 * are different messaging systems. Therefore if you want to send/receive a message with this messaging system
 * then output and input channels must be created by the same instance of ThreadMessagingSystemFactory.
 * <br/><br/>
 * Notice, the messages are always received in one particular working thread, but the notification events e.g. connection opened
 * are invoked in a different thread.
 *
 */
public class ThreadMessagingSystemFactory implements IMessagingSystemFactory
{
    /**
     * Constructs thread based messaging factory.
     * 
     * Every instance of the synchronous messaging system factory represents one messaging system.
     * It means that two instances of this factory creates channels for two independent (different) messaging system.
     */
    public ThreadMessagingSystemFactory()
    {
        this(new LocalProtocolFormatter());
    }
    
    /**
     * Constructs thread based messaging factory.
     * 
     * Every instance of the synchronous messaging system factory represents one messaging system.
     * It means that two instances of this factory creates channels for two independent (different) messaging system.
     * 
     * @param protocolFormatter low-level message formatter for the communication between channels.
     */
    public ThreadMessagingSystemFactory(IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDefaultMessagingFactory = new DefaultMessagingSystemFactory(new ThreadMessagingProvider(), protocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the output channel sending messages to the input channel and receiving response messages by using the working thread.
     * 
     */
    @Override
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
     * Creates the output channel sending messages to the input channel and receiving response messages by using the working thread.
     * 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
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
     * Creates the input channel receiving messages from the output channel and sending back response messages by using the working thread.
     *  
     */
    @Override
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
     * @return this ThreadMessagingSystemFactory
     */
    public ThreadMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myDefaultMessagingFactory.setInputChannelThreading(inputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return thread dispatcher
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myDefaultMessagingFactory.getInputChannelThreading();
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return ThreadMessagingSystemFactory
     */
    public ThreadMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myDefaultMessagingFactory.setOutputChannelThreading(outputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return thread dispatcher
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myDefaultMessagingFactory.getOutputChannelThreading();
    }
    
    private DefaultMessagingSystemFactory myDefaultMessagingFactory;
}
