/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.nodes.channelwrapper.*;

/**
 * Implents the factory creating broker and broker client.
 * The broker is the component that provides functionality for publish-subscribe scenarios.
 * IDuplexBrokerClient provides functionality to send notification messages to the broker
 * and also to subscribe for desired messages.
 * <br/>
 * <br/>
 * The example shows how to create and use the broker communicating via TCP.
 * <pre>
 * {@code
 * // Create Tcp based messaging.
 * IMessagingSystemFactory aMessagingFactory = new TcpMessagingSystemFactory();
 *
 * // Create duplex input channel listening to messages.
 * IDuplexInputChannel anInputChannel = aMessagingFactory.createDuplexInputChannel("tcp://127.0.0.1:7980/");
 *
 * // Create the factory for the broker.
 * IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
 *
 * // Create the broker.
 * IDuplexBroker aBroker = aBrokerFactory.createBroker();
 *
 * // Attach the Tcp duplex input channel to the broker and start listening.
 * aBroker.attachDuplexInputChannel(anInputChannel);
 * }
 * </pre>
 * <br/>
 * 
 * Subscribing for the notification messages.
 * <pre>
 * {@code
 * // Create Tcp based messaging for the silverlight client.
 * IMessagingSystemFactory aTcpMessagingFactory = new TcpMessagingSystemFactory();
 *
 * // Create duplex output channel to send and receive messages.
 * myOutputChannel = aTcpMessagingFactory.createDuplexOutputChannel("tcp://127.0.0.1:7980/");
 *
 * // Create the broker client
 * IDuplexBrokerFactory aDuplexBrokerFactory = new DuplexBrokerFactory();
 * myBrokerClient = aDuplexBrokerFactory.createBrokerClient();
 *
 * // Handler to process notification messages.
 * myBrokerClient.brokerMessageReceived().subscribe(myNotifyMessageReceived);
 *
 * // Attach the channel to the broker client to be able to send and receive messages.
 * myBrokerClient.attachDuplexOutputChannel(myOutputChannel);
 *
 * // Subscribe in broker to receive chat messages.
 * myBrokerClient.subscribe("MyChatMessageType");
 *
 *
 * ...
 *
 *
 * // Send message to the broker. The broker will then forward it to all subscribers.
 * XmlStringSerializer anXmlSerializer = new XmlStringSerializer();
 * Object aSerializedChatMessage = anXmlSerializer.Serialize(aChatMessage, ChatMessage.class);
 * myBrokerClient.sendMessage("MyChatMessageType", aSerializedChatMessage);
 *
 * }
 * </pre>
 * 
 * 
 * 
 *
 */
public class DuplexBrokerFactory implements IDuplexBrokerFactory
{
    /**
     * Constructs the broker factory with XmlStringSerializer.
     */
    public DuplexBrokerFactory()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelWrapperFactory = new ChannelWrapperFactory();
            myTypedRequestResponseFactory = new DuplexTypedMessagesFactory();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Constructs the broker factory with specified serializer.
     * @param serializer serializer used by the broker
     */
    public DuplexBrokerFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelWrapperFactory = new ChannelWrapperFactory(serializer);
            myTypedRequestResponseFactory = new DuplexTypedMessagesFactory(serializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexBrokerClient createBrokerClient() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexBrokerClient(new SynchronousMessagingSystemFactory(), myChannelWrapperFactory, myTypedRequestResponseFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexBroker createBroker() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexBroker(new SynchronousMessagingSystemFactory(), myChannelWrapperFactory, myTypedRequestResponseFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private IChannelWrapperFactory myChannelWrapperFactory;
    private IDuplexTypedMessagesFactory myTypedRequestResponseFactory;
}
