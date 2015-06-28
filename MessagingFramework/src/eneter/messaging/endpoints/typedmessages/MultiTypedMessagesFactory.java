/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.GetSerializerCallback;
import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;
import eneter.messaging.threading.dispatching.SyncDispatching;

/**
 * Factory to create multi-typed message senders and receivers.
 * 
 * The following example shows how to send and receive messages:
 * <br/>
 * Implementation of receiver (service):
 * <pre>
 * public class Program
 * {
 *     public static class MyRequestMessage
 *     {
 *         public double Number1;
 *         public double Number2;
 *     }
 *  <br/>
 *     public static void main(String[] args)
 *     {
 *         try
 *         {
 *             // Create multi-typed receiver.
 *             IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
 *             IMultiTypedMessageReceiver aReceiver = aFactory.createMultiTypedMessageReceiver();
 *  <br/>
 *             // Register message types which can be processed.
 *             aReceiver.registerRequestMessageReceiver(myIntegerHandler, Integer.class);
 *             aReceiver.registerRequestMessageReceiver(myMyRequestMessageHandler, MyRequestMessage.class);
 *  <br/>
 *             // Attach input channel and start listening e.g. using TCP.
 *             IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *             IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8033/");
 *             aReceiver.attachDuplexInputChannel(anInputChannel);
 *  <br/>
 *             System.out.println("Service is running. Press ENTER to stop.");
 *             new BufferedReader(new InputStreamReader(System.in)).readLine();
 *  <br/>
 *             // Detach input channel to stop the listening thread.
 *             aReceiver.detachDuplexInputChannel();
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("Service failed.", err);
 *         }
 *     }
 *  <br/>
 *     private static void onIntegerMessage(Object eventSender, TypedRequestReceivedEventArgs&lt;String&gt; e)
 *     {
 *         int aNumber = e.getRequestMessage();
 *  <br/>
 *         // Calculate factorial.
 *         int aResult = 1;
 *         for (int i = 1; i &lt;= aNumber; ++i)
 *         {
 *             aResult *= i;
 *         }
 *  <br/>
 *         System.out.println(aNumber + "! =" + aResult);
 *  <br/>
 *         // Send back the result.
 *         IMultiTypedMessageReceiver aReceiver = (IMultiTypedMessageReceiver)eventSender;
 *         try
 *         {
 *             aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult, Integer.class);
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("Failed to send the response message.", err);
 *         }
 *     }
 *  <br/>
 *     private static void onMyReqestMessage(Object eventSender, TypedRequestReceivedEventArgs&lt;MyRequestMessage&gt; e)
 *     {
 *         MyRequestMessage aRequestMessage = e.getRequestMessage();
 *  <br/>
 *         double aResult = aRequestMessage.Number1 + aRequestMessage.Number2;
 *  <br/>
 *         System.out.println(aRequestMessage.Number1 + " + " + aRequestMessage.Number2 + " = " + aResult);
 *  <br/>
 *         // Send back the message.
 *         IMultiTypedMessageReceiver aReceiver = (IMultiTypedMessageReceiver)eventSender;
 *         try
 *         {
 *             aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult, Double.class);
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("Failed to send the response message.", err);
 *         }
 *     }
 *  <br/>
 *     private static EventHandler&lt;TypedRequestReceivedEventArgs&lt;Integer&gt;&gt; myIntegerHandler =
 *             new EventHandler&lt;TypedRequestReceivedEventArgs&lt;Integer&gt;&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender, TypedRequestReceivedEventArgs&lt;Integer&gt; e)
 *         {
 *             onStringMessage(sender, e);
 *         }
 *     };
 *  <br/>
 *     private static EventHandler&lt;TypedRequestReceivedEventArgs&lt;MyRequestMessage&gt;&gt; myMyRequestMessageHandler =
 *             new EventHandler&lt;TypedRequestReceivedEventArgs&lt;MyRequestMessage&gt;&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender, TypedRequestReceivedEventArgs&lt;MyRequestMessage&gt; e)
 *         {
 *             onMyReqestMessage(sender, e);
 *         }
 *     };
 *  <br/>
 * }
 * </pre>
 * Implementation of sender (client):
 * <pre>
 * public class Program
 * {
 *     public static class MyRequestMessage
 *     {
 *         public double Number1;
 *         public double Number2;
 *     }
 *  <br/>
 *     public static void main(String[] args)
 *     {
 *         // Create multi-typed sender.
 *         IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
 *         ISyncMultitypedMessageSender aSender = aFactory.createSyncMultiTypedMessageSender();
 *  <br/>
 *         try
 *         {
 *             // Attach output channel and be able to communicate with the service.
 *             IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *             IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8033/");
 *             aSender.attachDuplexOutputChannel(anOutputChannel);
 *  <br/>
 *             // Request to calculate two numbers.
 *             MyRequestMessage aRequestMessage = new MyRequestMessage();
 *             aRequestMessage.Number1 = 10;
 *             aRequestMessage.Number2 = 20;
 *             double aResult = aSender.sendRequestMessage(aRequestMessage, Double.class, MyRequestMessage.class);
 *             System.out.println(aRequestMessage.Number1 + " + " + aRequestMessage.Number2 + " = " + aResult);
 *  <br/>
 *             // Request to calculate factorial.
 *             int aFactorial = aSender.sendRequestMessage((int)6, Integer.class, Integer.class);
 *             System.out.println("6! = " + aFactorial);
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("Calculating failed.", err);
 *         }
 *  <br/>
 *         // Detach input channel and stop listening to responses.
 *         aSender.detachDuplexOutputChannel();
 *     }
 *  <br/>
 * }
 * </pre>
 * 
 *
 */
public class MultiTypedMessagesFactory implements IMultiTypedMessagesFactory
{
    /**
     * Constructs the factory with default XmlSerializer.
     */
    public MultiTypedMessagesFactory()
    {
        this(new XmlStringSerializer());
    }
    
    /**
     * Constructs the factory.
     * @param serializer serializer which will serialize messages.
     */
    public MultiTypedMessagesFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySyncResponseReceiveTimeout = 0;
            mySerializer = serializer;
            mySerializerProvider = null;
            mySyncDuplexTypedSenderThreadMode = new SyncDispatching();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IMultiTypedMessageSender createMultiTypedMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MultiTypedMessageSender(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ISyncMultitypedMessageSender createSyncMultiTypedMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SyncMultiTypedMessageSender(mySyncResponseReceiveTimeout, mySerializer, mySyncDuplexTypedSenderThreadMode);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IMultiTypedMessageReceiver createMultiTypedMessageReceiver()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MultiTypedMessageReceiver(mySerializer, mySerializerProvider);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    /**
     * Sets the threading mode for receiving connectionOpened and connectionClosed events for SyncDuplexTypedMessageSender.
     * 
     * E.g. you use SyncDuplexTypedMessageSender and you want to route ConnectionOpened and ConnectionClosed events
     * to the main UI thread of your application. Therefore you specify WindowsDispatching when you create your
     * TCP duplex output channel which you then attach to the SyncDuplexTypedMessageSender.<br/>
     * Later when the application is running you call SyncDuplexTypedMessageSender.SendRequestMessage(..).<br/>
     * However if you call it from the main UI thread the deadlock occurs.
     * Because this component is synchronous the SendRequestMessage(..) will stop the calling main UI thread and will wait
     * for the response. But the problem is when the response comes the underlying TCP messaging will try to route it to
     * the main UI thread (as was specified during creating TCP duplex output channel).<br/>
     * But because the main UI thread is suspending and waiting the message will never arrive.<br/>
     * <br/>
     * Solution:<br/>
     * Do not specify the threading mode when you create yur duplex output channel but specify it using the
     * SyncDuplexTypedSenderThreadMode property when you create SyncDuplexTypedMessageSender.
     * 
     * @param threadingMode threading that shall be used for receiving connectionOpened and connectionClosed events.
     * @return instance of this DuplexTypedMessagesFactory
     */
    public MultiTypedMessagesFactory setSyncDuplexTypedSenderThreadMode(IThreadDispatcherProvider threadingMode)
    {
        mySyncDuplexTypedSenderThreadMode = threadingMode;
        return this;
    }
    
    /**
     * Gets the threading mode which is used for receiving connectionOpened and connectionClosed events in SyncDuplexTypedMessageSender.
     * @return
     */
    public IThreadDispatcherProvider getSyncDuplexTypedSenderThreadMode()
    {
        return mySyncDuplexTypedSenderThreadMode;
    }
    
    /**
     * Sets serializer for messages.
     * @param serializer serializer
     * @return this MultiTypedMessagesFactory
     */
    public MultiTypedMessagesFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    /**
     * Gets serializer for messages.
     * @return serializer
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    /**
     * Gets callback for retrieving serializer based on response receiver id.
     * This callback is used by MultiTypedMessageReceiver when it needs to serialize/deserialize the communication with MultiTypedMessageSender.
     * Providing this callback allows to use a different serializer for each connected client.
     * This can be used e.g. if the communication with each client needs to be encrypted using a different password.<br/>
     * <br/>
     * The default value is null and it means SerializerProvider callback is not used and one serializer which specified in the Serializer property is used for all serialization/deserialization.<br/>
     * If SerializerProvider is not null then the setting in the Serializer property is ignored.
     * @return GetSerializerCallback
     */
    public GetSerializerCallback getSerializerProvider()
    {
        return mySerializerProvider;
    }
    
    /**
     * Sets callback for retrieving serializer based on response receiver id.
     * This callback is used by MultiTypedMessageReceiver when it needs to serialize/deserialize the communication with MultiTypedMessageSender.
     * Providing this callback allows to use a different serializer for each connected client.
     * This can be used e.g. if the communication with each client needs to be encrypted using a different password.<br/>
     * <br/>
     * The default value is null and it means SerializerProvider callback is not used and one serializer which specified in the Serializer property is used for all serialization/deserialization.<br/>
     * If SerializerProvider is not null then the setting in the Serializer property is ignored.
     * @param serializerProvider
     * @return GetSerializerCallback
     */
    public MultiTypedMessagesFactory setSerializerProvider(GetSerializerCallback serializerProvider)
    {
        mySerializerProvider = serializerProvider;
        return this;
    }
    
    /**
     * Sets the timeout which is used for SyncMultitypedMessageSender.
     * When SyncMultitypedMessageSender calls sendRequestMessage(..) then it waits until the response is received.
     * This timeout specifies the maximum wating time. The default value is 0 and it means infinite time.
     * @param milliseconds timeout in milliseconds
     * @return this MultiTypedMessagesFactory
     */
    public MultiTypedMessagesFactory setSyncResponseReceiveTimeout(int milliseconds)
    {
        mySyncResponseReceiveTimeout = milliseconds;
        return this;
    }
    
    /**
     * Gets the timeout which is used for SyncMultitypedMessageSender.
     * When SyncMultitypedMessageSender calls sendRequestMessage(..) then it waits until the response is received.
     * This timeout specifies the maximum wating time. The default value is 0 and it means infinite time.
     * @return timeout in milliseconds
     */
    public int getSyncResponseReceiveTimeout()
    {
        return mySyncResponseReceiveTimeout;
    }
    
    
    private ISerializer mySerializer;
    private GetSerializerCallback mySerializerProvider;
    private int mySyncResponseReceiveTimeout;
    private IThreadDispatcherProvider mySyncDuplexTypedSenderThreadMode;
}
