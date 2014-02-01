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


/**
 * Implements the messaging system delivering messages to the particular working thread.
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
    public ThreadMessagingSystemFactory(IProtocolFormatter<?> protocolFormatter)
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
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using the working thread.
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
            return myDefaultMessagingFactory.createDuplexOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using the working thread.
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
            return myDefaultMessagingFactory.createDuplexOutputChannel(channelId, responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using the working thread.
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     * @throws Exception 
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

    @Override
    public IProtocolFormatter<?> getProtocolFormatter()
    {
        return myDefaultMessagingFactory.getProtocolFormatter();
    }
    
    private DefaultMessagingSystemFactory myDefaultMessagingFactory;
}
