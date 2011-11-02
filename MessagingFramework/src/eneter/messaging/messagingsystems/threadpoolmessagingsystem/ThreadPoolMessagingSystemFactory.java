/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.threadpoolmessagingsystem;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.*;


/**
 * Implements the messaging system delivering messages with using .Net thread pool.
 * The messages are put to the queue of .Net thread pool. The receiving input channel is then called
 * in the context of the assigned thread from the pool. Therefore the input channel can process more messages at once
 * and also can notify the subscriber from more different threads at the same time. <br/>
 * <b>Therefore do not forget to be careful and avoid race conditioning.</b>
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public class ThreadPoolMessagingSystemFactory implements IMessagingSystemFactory
{
    public ThreadPoolMessagingSystemFactory()
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            myMessagingSystem = new SimpleMessagingSystem(new ThreadPoolMessagingProvider());
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }
    
    /**
     * Creates the output channel sending messages to the specified input channel via the thread pool.
     * The output channel can send messages only to the input channel and not to the duplex input channel.
     */
    @Override
    public IOutputChannel createOutputChannel(String channelId)
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            return new SimpleOutputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }

    /**
     * Creates the input channel receiving messages from the output channel via the thread pool.
     * The input channel can receive messages only from the output channel and not from the duplex output channel.
     * 
     */
    @Override
    public IInputChannel createInputChannel(String channelId)
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            return new SimpleInputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using the thread pool.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     */
    @Override
    public IDuplexOutputChannel CreateDuplexOutputChannel(String channelId)
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, null, this);
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using the thread pool.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     */
    @Override
    public IDuplexOutputChannel CreateDuplexOutputChannel(String channelId,
            String responseReceiverId)
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, responseReceiverId, this);
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using the thread pool.
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     */
    @Override
    public IDuplexInputChannel CreateDuplexInputChannel(String channelId)
    {
        AutoCloseable aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexInputChannel(channelId, this);
        }
        finally
        {
            if (aTrace != null) aTrace.close();
        }
    }

    
    private IMessagingSystemBase myMessagingSystem;
}
