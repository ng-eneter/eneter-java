/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.threadpoolmessagingsystem;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;


/**
 * Messaging system delivering messages asynchronously (when a message is received a separate thread is invoked to process it).
 * 
 * The incoming messages are processed by multiple threads from the pool. When a message is received the thread
 * from the pool is taken and the message is notified.
 * Therefore messages come asynchronously in various threads.
 * 
 */
public class ThreadPoolMessagingSystemFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the factory.
     * 
     */
    public ThreadPoolMessagingSystemFactory()
    {
        this(new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the factory.
     * 
     * 
     * @param protocolFormatter formatting of low-level messages between output and input channels.
     */
    public ThreadPoolMessagingSystemFactory(IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySimpleMessagingFactory = new DefaultMessagingSystemFactory(new ThreadPoolMessagingProvider(), protocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return mySimpleMessagingFactory.createDuplexOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return mySimpleMessagingFactory.createDuplexOutputChannel(channelId, responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return mySimpleMessagingFactory.createDuplexInputChannel(channelId);
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
    public ThreadPoolMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        mySimpleMessagingFactory.setInputChannelThreading(inputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return mySimpleMessagingFactory.getInputChannelThreading();
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return
     */
    public ThreadPoolMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        mySimpleMessagingFactory.setOutputChannelThreading(outputChannelThreading);
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return mySimpleMessagingFactory.getOutputChannelThreading();
    }
    
    private DefaultMessagingSystemFactory mySimpleMessagingFactory;
}
