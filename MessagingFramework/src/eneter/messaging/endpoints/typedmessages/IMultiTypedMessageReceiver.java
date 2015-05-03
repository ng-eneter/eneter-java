/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;

/**
 * Receiver for multiple message types.
 * 
 * It is a service component which can receive and send messages of multiple types.<br/>
 * The following example shows how to create a service which can receive messages of various types:
 * <pre>
 * {@code
 * // Create multityped receiver
 * IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
 * IMultiTypedMessageReceiver aReceiver = aFactory.createMultiTypedMessageReceiver();
 * 
 * // Register handlers for message types which can be received.
 * aReceiver.registerRequestMessageReceiver(myAlarmHandler, Alarm.class);
 * aReceiver.registerRequestMessageReceiver(myImageHandler, Image.class);
 * 
 * // Attach input channel and start listening. E.g. using TCP.
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:9043/");
 * aReceiver.attachDuplexInputChannel(anInputChannel);
 * 
 * System.out.println("Service is running. Press ENTER to stop.");
 * new BufferedReader(new InputStreamReader(System.in)).readLine();
 * 
 * // Detach input channel and stop listening.
 * aReceiver.detachInputChannel();
 * 
 * }
 * </pre>
 * 
 * The following code demonstrates how to implement handlers:
 * <pre>
 * {@code
 * private void onAlarmMessage(object sender, TypedRequestReceivedEventArgs<Alarm> e)
 * {
 *    // Get alarm message data.
 *    Alarm anAlarm = e.getRequestMessage();
 *    
 *     ....
 *     
 *    // Send response message.
 *    aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMessage, ResponseMessage.class);
 * }
 * 
 * private void onImageMessage(object sender, TypedRequestReceivedEventArgs<Image> e)
 * {
 *    // Get image message data.
 *    Image anImage = e.getRequestMessage();
 *    
 *     ....
 *     
 *    // Send response message.
 *    aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMessage, ResponseMessage.class);
 * }
 * 
 * 
 * private EventHandler<TypedRequestReceivedEventArgs<Alarm>> myAlarmHandler =
 *     new EventHandler<TypedRequestReceivedEventArgs<Alarm>>()
 *     {
 *         public void onEvent(Object sender, TypedRequestReceivedEventArgs<Alarm> e)
 *         {
 *             onAlarmMessage(sender, e);
 *         }
 *     };
 *     
 * private EventHandler<TypedRequestReceivedEventArgs<Image>> myImageHandler =
 *     new EventHandler<TypedRequestReceivedEventArgs<Image>>()
 *     {
 *         public void onEvent(Object sender, TypedRequestReceivedEventArgs<Image> e)
 *         {
 *             onImageMessage(sender, e);
 *         }
 *     };
 *   
 * }
 * </pre>
 *
 */
public interface IMultiTypedMessageReceiver extends IAttachableDuplexInputChannel
{
    /**
     * Raised when a new client is connected.
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * Raised when a client closed the connection.
     * The event is raised only if the connection was closed by the client.
     * It is not raised if the client was disconnected by IDuplexInputChannel.disconnectResponseReceiver(...). 
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Registers message handler for specified message type.
     * If the specified message type is received the handler will be called to process it.
     *  
     * @param handler message handler which shall be called when the specified message type is received.
     * @param clazz type of the message.
     * @throws Exception
     */
    <T> void registerRequestMessageReceiver(EventHandler<TypedRequestReceivedEventArgs<T>> handler, Class<T> clazz) throws Exception;
    
    /**
     * Unregisters the message handler for the specified message type.
     * @param clazz type of the message.
     */
    <T> void unregisterRequestMessageReceiver(Class<T> clazz);
    
    /**
     * Returns the list of registered message types which can be received. 
     * @return registered message types
     */
    ArrayList<Class<?>> getRegisteredRequestMessageTypes();
    
    /**
     * Sends the response message.
     * 
     * The message of the specified type will be serialized and sent back to the response receiver.
     * If the response receiver has registered a handler for this message type then the handler will be called to process the message.
     * 
     * @param responseReceiverId identifies the client. If responseReceiverId is * then the broadcast message
     * to all connected clients is sent.
     * <pre>
     * // Send broadcast to all connected clients.
     * aReceiver.sendResponseMessage("*", aBroadcastMessage, YourBroadcast.class);
     * </pre>
     * @param responseMessage response message
     * @param clazz type of the response message
     * @throws Exception
     * 
     * 
     */
    <TResponseMessage> void sendResponseMessage(String responseReceiverId, TResponseMessage responseMessage, Class<TResponseMessage> clazz) throws Exception;
}
