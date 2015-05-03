/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;

/**
 * Publishes and subscribes messages in the broker.
 * 
 * The broker client is the component which interacts with the broker.
 * It allows to publish messages via the broker and to subscribe for desired messages in the broker.<br/>
 * <br/>
 * The following example shows how to subscribe a message in the broker:
 * <pre>
 * {@code
 * // Create the broker client.
 * IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
 * IDuplexBrokerClient aBrokerClient = aBrokerFactory.createBrokerClient();
 * 
 * // Register handler to process subscribed messages from the broker.
 * aBrokerClient.brokerMessageReceived().subscribe(myMessageHandler);
 * 
 * // Attach output channel and be able to communicate with the broker.
 * // E.g. if the broker communicates via TCP. 
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:9843/");
 * aBrokerClient.attachDuplexOutputChannel(anOutputChannel);
 * 
 * // Now when the connection with the broker is establish so we can subscribe for
 * // messages in the broker.
 * // After this call whenever somebody sends the message type 'MyMessageType' into the broker
 * // the broker will forward it to this broker client and the message handler
 * // myMessageHandler will be called.
 * aBrokerClient.subscribe("MyMessageType");
 *  
 * }
 * </pre>
 * <br/>
 * The following example shows how to publish a message via the broker:
 * <pre>
 * {@code
 * // Create the broker client.
 * IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
 * IDuplexBrokerClient aBrokerClient = aBrokerFactory.createBrokerClient();
 * 
 * // Attach output channel and be able to communicate with the broker.
 * // E.g. if the broker communicates via TCP. 
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:9843/");
 * aBrokerClient.attachDuplexOutputChannel(anOutputChannel);
 * 
 * // Now when the connection with the broker is establish so we can publish the message.
 * // Note: the broker will receive the message and will forward it to everybody who is subscribed for MyMessageType.
 * aBrokerClient.sendMessage("MyMessageType", "Hello world.");
 *  
 * }
 * </pre>
 *
 */
public interface IDuplexBrokerClient extends IAttachableDuplexOutputChannel
{
    /**
     * Event raised when the connection with the service was open.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Event raised when the connection with the service was closed.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * Event raised when a subscribed message type is received from the broker.
     * @return
     */
    Event<BrokerMessageReceivedEventArgs> brokerMessageReceived();
    
    /**
     * Publishes the message via the broker.
     * @param messageType event identifier
     * @param serializedMessage message content.
     * @throws Exception
     */
    void sendMessage(String messageType, Object serializedMessage) throws Exception;
    
    /**
     * Subscribes for the message type.
     * 
     * If you can call this method multiple times to subscribe for multiple events.
     * 
     * @param messageType identifies the type of the message which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String messageType) throws Exception;
    
    /**
     * Subscribes for list of message types.
     * 
     * @param messageType list of message types which shall be subscribed.
     * @throws Exception
     */
    void subscribe(String[] messageType) throws Exception;
    
    /**
     * Unsubscribes from the specified message type.
     * @param messageType message type the client does not want to receive anymore.
     * @throws Exception
     */
    void unsubscribe(String messageType) throws Exception;
    
    /**
     * Unsubscribes from specified message types.
     * @param messageTypes list of message types the client does not want to receive anymore.
     * @throws Exception
     */
    void unsubscribe(String[] messageTypes) throws Exception;
    
    /**
     * Unsubscribe all messages.
     * @throws Exception
     */
    void unsubscribe() throws Exception;
}
