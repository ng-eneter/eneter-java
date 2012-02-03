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
        this(new EneterProtocolFormatter());
    }
    
    public ThreadPoolMessagingSystemFactory(IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingSystem = new SimpleMessagingSystem(new ThreadPoolMessagingProvider());
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the output channel sending messages to the specified input channel via the thread pool.
     * The output channel can send messages only to the input channel and not to the duplex input channel.
     */
    @Override
    public IOutputChannel createOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleOutputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
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
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleInputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
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
     * @throws Exception 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, null, this, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
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
     * @throws Exception 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, responseReceiverId, this, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
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
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexInputChannel(channelId, this, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemBase myMessagingSystem;
    private IProtocolFormatter<?> myProtocolFormatter;
}
