/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Creates broker and broker client.
 * The broker is the component for publish-subscribe scenarios. It maintains the list of subscribers.
 * When it receives a notification message it forwards it to subscribed clients. 
 *   
 * IDuplexBrokerClient can send notification messages to the broker
 * and also to subscribe for desired messages.
 * <br/>
 * <br/>
 * The example shows how to create and use the broker via TCP.
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
     * Constructs the broker factory with optimized custom serializer.
     * 
     */
    public DuplexBrokerFactory()
    {
        this(new BrokerCustomSerializer());
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
            myIsPublisherNotified = true;
            mySerializer = serializer;
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
            return new DuplexBrokerClient(mySerializer);
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
            return new DuplexBroker(myIsPublisherNotified, mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets the serializer to serialize/deserialize {@link BrokerMessate}.
     * {@link BrokerMessate} is used for the communication with the broker.
     * @param serializer serializer
     * @return this DuplexBrokerFactory
     */
    public DuplexBrokerFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    /**
     * Returns the serializer which is used to serialize/deserialize {@link BrokerMessate}.
     * @return
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    /**
     * Sets the flag whether the publisher which sent a message shall be notified in case it is subscribed to the same message.
     * 
     * When a DuplexBrokerClient sent a message the broker forwards the message to all subscribed DuplexBrokerClients.
     * In case the DuplexBrokerClient is subscribed to the same message the broker will notify it if the flag
     * IsBublisherNotified is set to true.
     * If it is set to false then the broker will not forward the message to the DuplexBrokerClient which
     * published the message.
     * 
     * @param isPublisherNotified true if the DuplexBrokerClient which sent the message shall be notified too.
     * @return this DuplexBrokerFactory
     */
    public DuplexBrokerFactory setIsPublisherNotified(boolean isPublisherNotified)
    {
        myIsPublisherNotified = isPublisherNotified;
        return this;
    }
    
    /**
     * Gets the flag whether the publisher which sent a message shall be notified in case it is subscribed to the same message.
     * 
     * When a DuplexBrokerClient sent a message the broker forwards the message to all subscribed DuplexBrokerClients.
     * In case the DuplexBrokerClient is subscribed to the same message the broker will notify it if the flag
     * IsBublisherNotified is set to true.
     * If it is set to false then the broker will not forward the message to the DuplexBrokerClient which
     * published the message.
     * 
     * @return
     */
    public boolean getIsPublisherNotified()
    {
        return myIsPublisherNotified;
    }
    
    
    private ISerializer mySerializer;
    private boolean myIsPublisherNotified;
}
