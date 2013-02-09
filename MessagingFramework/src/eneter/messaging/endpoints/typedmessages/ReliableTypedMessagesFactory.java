/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the factory to create reliable typed message sender and receiver.
 *
 * The reliable messaging means that the sender of a message is notified whether
 * the message was delivered or not.<br/>
 * <br/>
 * Example of a simple service using the reliable messaging for the communication.
 * (The service calculates two numbers and sends back responses.)
 * <pre>
 * public class ProgramReliableCalculatorService
 * {
 *     // Request message.
 *     public static class RequestMessage
 *     {
 *         public int Number1;
 *         public int Number2;
 *     }
 * 
 *     // Response message.
 *     public static class ResponseMessage
 *     {
 *         public int Result;
 *     }
 * 
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Create reliable message receiver.
 *         IReliableTypedMessagesFactory aReceiverFactory = new ReliableTypedMessagesFactory();
 *         IReliableTypedMessageReceiver&lt;ResponseMessage, RequestMessage&gt; aReceiver =
 *             aReceiverFactory.createReliableDuplexTypedMessageReceiver(ResponseMessage.class, RequestMessage.class);
 * 
 *         // Subscribe to be notified whether sent response messages
 *         // were received.
 *         aReceiver.responseMessageDelivered().subscribe(myOnResponseMessageDelivered);
 *         aReceiver.responseMessageNotDelivered().subscribe(myOnResponseMessageNotDelivered);
 * 
 *         // Subscribe to process request messages.
 *         aReceiver.messageReceived().subscribe(myOnRequestReceived);
 * 
 *         // Use WebSocket for the communication.
 *         // Note: You can also other messagings. E.g. TcpMessagingSystemFactory
 *         IMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
 *         IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("ws://127.0.0.1:8099/aaa/");
 * 
 *         // Attach the input channel to the receiver and start listening.
 *         aReceiver.attachDuplexInputChannel(anInputChannel);
 *         
 *         System.out.println("Calculator service is running. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *         
 *         // Detach the input channel to stop listening.
 *         aReceiver.detachDuplexInputChannel();
 *     }
 *     
 *     private static void onRequestReceived(Object sender, TypedRequestReceivedEventArgs&lt;RequestMessage&gt; e)
 *     {
 *         // Calculate numbers.
 *         ResponseMessage aResponseMessage = new ResponseMessage();
 *         aResponseMessage.Result = e.getRequestMessage().Number1 + e.getRequestMessage().Number2;
 * 
 *         System.out.println(e.getRequestMessage().Number1 + " + " + e.getRequestMessage().Number2 + " = " + Integer.toString(aResponseMessage.Result));
 * 
 *         // Send back the response message.
 *         IReliableTypedMessageReceiver&lt;ResponseMessage, RequestMessage&gt; aReceiver = (IReliableTypedMessageReceiver&lt;ResponseMessage, RequestMessage&gt;)sender;
 *         
 *         try
 *         {
 *             String aResponseId = aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMessage);
 *             System.out.println("Sent response has Id: " + aResponseId);
 *         }
 *         catch (Exception e1)
 *         {
 *             e1.printStackTrace();
 *         }
 *     }
 * 
 *     private static void onResponseMessageDelivered(Object sender, ReliableMessageIdEventArgs e)
 *     {
 *         System.out.println("Response Id: " + e.getMessageId() + " was delivered.");
 *     }
 *     
 *     private static void onResponseMessageNotDelivered(Object sender, ReliableMessageIdEventArgs e)
 *     {
 *         System.out.println("Response Id: " + e.getMessageId() + " was NOT delivered.");
 *     }
 *     
 *     private static EventHandler&lt;TypedRequestReceivedEventArgs&lt;RequestMessage&gt;&gt; myOnRequestReceived = new EventHandler&lt;TypedRequestReceivedEventArgs&lt;RequestMessage&gt;&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender,
 *                 TypedRequestReceivedEventArgs&lt;RequestMessage&gt; e)
 *         {
 *             onRequestReceived(sender, e);
 *         }
 *     };
 *     
 *     private static EventHandler&lt;ReliableMessageIdEventArgs&gt; myOnResponseMessageDelivered = new EventHandler&lt;ReliableMessageIdEventArgs&gt;()
 *         {
 *             {@literal @}Override
 *             public void onEvent(Object sender, ReliableMessageIdEventArgs e)
 *             {
 *                 onResponseMessageDelivered(myOnResponseMessageDelivered, e);
 *             }
 *         };
 *     
 *     private static EventHandler&lt;ReliableMessageIdEventArgs&gt; myOnResponseMessageNotDelivered = new EventHandler&lt;ReliableMessageIdEventArgs&gt;()
 *         {
 *             {@literal @}Override
 *             public void onEvent(Object sender, ReliableMessageIdEventArgs e)
 *             {
 *                 onResponseMessageNotDelivered(myOnResponseMessageDelivered, e);
 *             }
 *         };
 * }
 * 
 * </pre>
 * 
 * Example showing a simple client using reliable messaging for the communication.
 * (The client sends a request to calculate two numbers.)
 * <pre>
 * public class ProgramReliableCalculatorClient
 * {
 *     // Request message.
 *     public static class RequestMessage
 *     {
 *         public int Number1;
 *         public int Number2;
 *     }
 * 
 *     // Response message.
 *     public static class ResponseMessage
 *     {
 *         public int Result;
 *     }
 *     
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Create the message sender.
 *         IReliableTypedMessagesFactory aSenderFactory = new ReliableTypedMessagesFactory();
 *         IReliableTypedMessageSender&lt;ResponseMessage, RequestMessage&gt; aSender 
 *             = aSenderFactory.createReliableDuplexTypedMessageSender(ResponseMessage.class, RequestMessage.class);
 * 
 *         // Subscribe to be notified whether the request message was delivered or not.
 *         aSender.messageDelivered().subscribe(myOnResponseMessageDelivered);
 *         aSender.messageNotDelivered().subscribe(myOnResponseMessageNotDelivered);
 * 
 *         // Subscribe to receive response messages.
 *         aSender.responseReceived().subscribe(myOnResponseReceived);
 * 
 *         // Use Websocket for the communication.
 *         // If you want to use TCP then use TcpMessagingSystemFactory().
 *         IMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
 *         IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8099/aaa/");
 * 
 *         // Attach the output channel and be able to send messages
 *         // and receive response messages.
 *         aSender.attachDuplexOutputChannel(anOutputChannel);
 *         
 *         Scanner aReader = new Scanner(System.in);
 *         
 *         RequestMessage aRequestMessage = new RequestMessage();
 *         
 *         // Ask user to put values.
 *         System.out.println("Number1 = ");
 *         aRequestMessage.Number1 = aReader.nextInt();
 *         
 *         System.out.println("Number2 = ");
 *         aRequestMessage.Number2 = aReader.nextInt();
 *         
 *         // Send the request to calculate given numbers.
 *         String aMessageId = aSender.sendRequestMessage(aRequestMessage);
 *         System.out.println("Sent request has id: " + aMessageId);
 *         
 *         System.out.println("Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 * 
 *         aSender.detachDuplexOutputChannel();
 *     }
 *     
 *     private static void onResponseReceived(Object sender, TypedResponseReceivedEventArgs&lt;ResponseMessage&gt; e)
 *     {
 *         System.out.println("Result =  " + Integer.toString(e.getResponseMessage().Result));
 *     }
 * 
 *     private static void onResponseMessageDelivered(Object sender, ReliableMessageIdEventArgs e)
 *     {
 *         System.out.println("Response Id: " + e.getMessageId() + " was delivered.");
 *     }
 *     
 *     private static void onResponseMessageNotDelivered(Object sender, ReliableMessageIdEventArgs e)
 *     {
 *         System.out.println("Response Id: " + e.getMessageId() + " was NOT delivered.");
 *     }
 *     
 *     
 *     private static EventHandler&lt;TypedResponseReceivedEventArgs&lt;ResponseMessage&gt;&gt; myOnResponseReceived = new EventHandler&lt;TypedResponseReceivedEventArgs&lt;ResponseMessage&gt;&gt;()
 *         {
 *             {@literal @}Override
 *             public void onEvent(Object sender,
 *                     TypedResponseReceivedEventArgs&lt;ResponseMessage&gt; e)
 *             {
 *                 onResponseReceived(sender, e);
 *             }
 *         };
 *     
 *     private static EventHandler&lt;ReliableMessageIdEventArgs&gt; myOnResponseMessageDelivered = new EventHandler&lt;ReliableMessageIdEventArgs&gt;()
 *         {
 *             {@literal @}Override
 *             public void onEvent(Object sender, ReliableMessageIdEventArgs e)
 *             {
 *                 onResponseMessageDelivered(myOnResponseMessageDelivered, e);
 *             }
 *         };
 *         
 *     private static EventHandler&lt;ReliableMessageIdEventArgs&gt; myOnResponseMessageNotDelivered = new EventHandler&lt;ReliableMessageIdEventArgs&gt;()
 *         {
 *             {@literal @}Override
 *             public void onEvent(Object sender, ReliableMessageIdEventArgs e)
 *             {
 *                 onResponseMessageNotDelivered(myOnResponseMessageDelivered, e);
 *             }
 *         };
 * }
 * 
 * </pre>
 */
public class ReliableTypedMessagesFactory implements IReliableTypedMessagesFactory
{
    /**
     * Constructs the factory with default settings.
     * 
     * For the serialization of reliable messages is used XmlStringSerializer.
     * The maximum time, the acknowledge message must be received is set to 12 seconds.
     */
    public ReliableTypedMessagesFactory()
    {
        this(12000, new XmlStringSerializer());
    }
    
    /**
     * Constructs the factory.
     * 
     * For the serialization of reliable messages is used XmlStringSerializer.
     * 
     * @param acknowledgeTimeout The maximum time until the delivery of the message must be acknowledged.
     */
    public ReliableTypedMessagesFactory(int acknowledgeTimeout)
    {
        this(acknowledgeTimeout, new XmlStringSerializer());
    }
    
    /**
     * Constructs the factory.
     * @param acknowledgeTimeout The maximum time until the delivery of the message must be acknowledged.
     * @param serializer Serializer used to serialize messages.
     */
    public ReliableTypedMessagesFactory(int acknowledgeTimeout, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAcknowledgeTimeout = acknowledgeTimeout;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * Creates the reliable message sender.
     */
    @Override
    public <_ResponseType, _RequestType> IReliableTypedMessageSender<_ResponseType, _RequestType> createReliableDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ReliableDuplexTypedMessageSender<_ResponseType, _RequestType>(myAcknowledgeTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the reliable message receiver.
     */
    @Override
    public <_ResponseType, _RequestType> IReliableTypedMessageReceiver<_ResponseType, _RequestType> createReliableDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new ReliableDuplexTypedMessageReceiver<_ResponseType, _RequestType>(myAcknowledgeTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private int myAcknowledgeTimeout;
    private ISerializer mySerializer;
}
