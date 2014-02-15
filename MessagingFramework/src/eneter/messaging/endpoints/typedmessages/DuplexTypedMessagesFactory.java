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
 * Implements the factory to create typed message senders and receivers.
 * 
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
 *
 */
public class DuplexTypedMessagesFactory implements IDuplexTypedMessagesFactory
{
    /**
     * Constructs the factory.
     */
    public DuplexTypedMessagesFactory()
    {
        this(0, new XmlStringSerializer());
    }

    /**
     * Constructs the factory with specified timeout for synchronous messaging.
     * @param syncResponseReceiveTimeout maximum waiting time when synchronous message sender is used. 
     */
    public DuplexTypedMessagesFactory(int syncResponseReceiveTimeout)
    {
        this(syncResponseReceiveTimeout, new XmlStringSerializer());
    }
    
    /**
     * Constructs the factory with specified serializer that will be used to serialize/deserialize messages.
     * @param serializer serializer that will be used to serialize/deserialize messages.
     */
    public DuplexTypedMessagesFactory(ISerializer serializer)
    {
        this(0, serializer);
    }
    
    /**
     * Constructs the factory with specified parameters.
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
    public <_ResponseType, _RequestType> IDuplexTypedMessageSender<_ResponseType, _RequestType> createDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageSender<_ResponseType, _RequestType>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <_ResponseType, _RequestType> ISyncDuplexTypedMessageSender<_ResponseType, _RequestType> createSyncDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SyncTypedMessageSender<_ResponseType, _RequestType> aSender = new SyncTypedMessageSender<_ResponseType, _RequestType>(mySyncResponseReceiveTimeout, mySerializer, responseMessageClazz, requestMessageClazz);
            return aSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex typed message receiver.
     */
    @Override
    public <_ResponseType, _RequestType> IDuplexTypedMessageReceiver<_ResponseType, _RequestType> createDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexTypedMessageReceiver<_ResponseType, _RequestType>(mySerializer, responseMessageClazz, requestMessageClazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer mySerializer;
    private int mySyncResponseReceiveTimeout;
}
