/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Functionality to send and receive messages of specified data type. If XmlStringSerializer is used
 * the typed messages can be sent between applications running on different platforms. E.g. you can
 * send ryped messages between Android and .NET applications.<br/>
 * <br/>
 * The following example shows how to send a typed message via TCP.<br/>
 * <pre>
 * {@code
 * // Message to be sent and received.
 * public class Item
 * {
 *      public String name;
 *      public int amount;
 * }
 *
 * class MySender
 * {
 *      // Typed message sender sending 'Item' and as a response receiving 'String'.
 *      private IDuplexTypedMessageSender<String, Item> mySender;
 *
 *      public void openConnection() throws Exception
 *      {
 *          // Create message sender sending 'Item' and receiving 'String'.
 *          // Note: XmlStringSerializer is used by default.
 *          IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
 *          mySender = aSenderFactory.createDuplexTypedMessageSender(String.class, Item.class);
 *
 *          // Subscribe to receive response messages.
 *          mySender.responseReceived().subscribe(myOnResponseHandler);
 *
 *          // Create TCP messaging.
 *          IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *          IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8033/");
 *
 *          // Attach output channel and be able to send messages and receive responses.
 *          mySender.attachDuplexOutputChannel(anOutputChannel);
 *      }
 *
 *      public void closeConnection()
 *      {
 *          // Detach output channel and stop listening to response messages.
 *          mySender.detachDuplexOutputChannel();
 *      }
 *
 *      public void sendMessage(Item message) throws Exception
 *      {
 *          mySender.sendMessage(message);
 *      }
 *
 *      private void onResponseReceived(Object sender, TypedResponseReceivedEventArgs<String> e)
 *      {
 *          // Get the response message.
 *          String aReceivedResponse = e.getResponseMessage();
 *          ...
 *      }
 *
 *
 *      // Response message handler
 *      EventHandler<TypedResponseReceivedEventArgs<String>> myOnResponseHandler = new EventHandler<TypedResponseReceivedEventArgs<String>>()
 *      {
 *          public void invoke(Object x, TypedRequestReceivedEventArgs<String> y)
 *              throws Exception
 *          {
 *              onResponseReceived(x, y);
 *          }
 *      }
 * }
 * }
 * </pre>
 * <br/>
 * The following example shows how to receive messages of specified type.<br/>
 * <pre>
 * {@code
 * class MyReceiver
 * {
 *      // Typed message receiver receiving 'Item' and responding 'String'.
 *      private IDuplexTypedMessageReceiver<String, Item> myReceiver;
 *
 *      public void startListening() throws Exception
 *      {
 *          // Create message receiver receiving 'Item' and responding 'String'.
 *          // Note: XmlStringSerializer is used by default.
 *          IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory();
 *          myReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(String.class, Item.class);
 *
 *          // Subscribe to receive messages.
 *          myReceiver.messageReceived().subscribe(myOnMessageHandler);
 *
 *          // Create TCP messaging.
 *          IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *          IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8033/");
 *
 *          // Attach input channel and start listening to messages.
 *          myReceiver.attachDuplexInputChannel(anInputChannel);
 *      }
 *
 *      public void stopListening()
 *      {
 *          // Detach input channel and stop listening.
 *          myReceiver.detachDuplexInputChannel();
 *      }
 *
 *
 *      private void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<Item> e)
 *          throws Exception
 *      {
 *          // Get the response message.
 *          Item anItem = e.getMessage();
 *          ...
 *          // Send back a response message.
 *          // Note: The response is declared as String.
 *          String aResponseMsg = "Response message.";
 *          myReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMsg);
 *      }
 *
 *
 *      // Received message handler
 *      EventHandler<TypedRequestReceivedEventArgs<Item>> myOnResponseHandler = new EventHandler<TypedRequestReceivedEventArgs<Item>>()
 *      {
 *          public void invoke(Object x, TypedRequestReceivedEventArgs<Item> y)
 *              throws Exception
 *          {
 *              onMessageReceived(x, y);
 *          }
 *      }
 * }
 * }
 * </pre>
 */
package eneter.messaging.endpoints.typedmessages;

