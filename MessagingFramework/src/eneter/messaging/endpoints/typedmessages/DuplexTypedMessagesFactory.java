/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;


/**
 * Factory to create typed message senders and receivers.
 * <br/>
 * The following example shows how to send a receive messages:
 * <br/>
 * <br/>
 * Implementation of receiver:
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
 * <br/>
 * Implementation of sender:
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
 * Implementation of synchronous sender (after sending it waits for the response):
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
 *      private ISyncDuplexTypedMessageSender<String, Item> mySender;
 *
 *      public void openConnection() throws Exception
 *      {
 *          // Create message sender sending 'Item' and receiving 'String'.
 *          // Note: XmlStringSerializer is used by default.
 *          IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
 *          mySender = aSenderFactory.createSyncDuplexTypedMessageSender(String.class, Item.class);
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
 *      // Sends message and waits for the response.
 *      public String sendMessage(Item message) throws Exception
 *      {
 *          String aResponse = mySender.sendMessage(message);
 *          return aResponse;
 *      }
 * }
 * }
 * </pre>
 */
public class DuplexTypedMessagesFactory implements IDuplexTypedMessagesFactory
{
    /**
     * Constructs the factory with XmlStringSerializer.
     * 
     * The factory will create senders and receivers with the default XmlStringSerializer
     * and the factory will create ISyncDuplexTypedMessageSender that waits infinite
     * time for the response message from the service.
     */
    public DuplexTypedMessagesFactory()
    {
        this(0, new XmlStringSerializer());
    }

    /**
     * Constructs the factory with specified serializer.
     * 
     * The factory will create senders and receivers with the specified serializer
     * and the factory will create ISyncDuplexTypedMessageSender that can wait infinite
     * time for the response message from the service.<br/>
     * <br/>
     * For possible serializers you can refer to {@link eneter.messaging.dataprocessing.serializing}
     * @param serializer serializer that will be used to serialize/deserialize messages.
     */
    public DuplexTypedMessagesFactory(ISerializer serializer)
    {
        this(0, serializer);
    }
    
    /**
     * Constructs the factory with specified timeout for {@link ISyncDuplexTypedMessageSender}.
     * 
     * The factory will create senders and receivers using the default XmlStringSerializer
     * and the factory will create ISyncDuplexTypedMessageSender with specified timeout
     * indicating how long it can wait for a response message from the service.
     * 
     * @param syncResponseReceiveTimeout maximum waiting time when synchronous message sender is used. 
     */
    public DuplexTypedMessagesFactory(int syncResponseReceiveTimeout)
    {
        this(syncResponseReceiveTimeout, new XmlStringSerializer());
    }
    
    
    
    /**
     * Constructs the factory with specified timeout for synchronous message sender and specified serializer.
     * @param syncResponseReceiveTimeout maximum waiting time when synchronous message sender is used.
     * @param serializer serializer that will be used to serialize/deserialize messages.
     */
    public DuplexTypedMessagesFactory(int syncResponseReceiveTimeout, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            mySyncResponseReceiveTimeout = syncResponseReceiveTimeout;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <TResponse, TRequest> IDuplexTypedMessageSender<TResponse, TRequest> createDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageSender<TResponse, TRequest>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <TResponse, TRequest> ISyncDuplexTypedMessageSender<TResponse, TRequest> createSyncDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SyncTypedMessageSender<TResponse, TRequest> aSender = new SyncTypedMessageSender<TResponse, TRequest>(mySyncResponseReceiveTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
            return aSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TResponse, TRequest> IDuplexTypedMessageReceiver<TResponse, TRequest> createDuplexTypedMessageReceiver(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageReceiver<TResponse, TRequest>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer mySerializer;
    private int mySyncResponseReceiveTimeout;
}
