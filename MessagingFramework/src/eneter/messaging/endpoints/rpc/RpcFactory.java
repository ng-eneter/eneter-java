/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.*;
import eneter.net.system.IFunction;



/**
 * Creates services and clients that can communicate using RPC (Remote Procedure Calls).
 * 
 * RPC is the communication scenario where an application (typically client) executes a method in another application (typically service). 
 * RpcFactory provides methods to instantiate RpcService and RpcClient objects.
 * 
 * RpcService acts as a stub which provides the communication functionality allowing the service to be reached from outside.
 * RpcClient acts as a proxy which provides the communication functionality allowing the client to call remote methods in the service.
 * 
 * The following example shows simple client-service communication using RPC.
 * 
 * The service side:
 * <pre>
 * public interface IHello
 * {
 *     // Event that can be subscribed from the client.
 *     Event&lt;MyEventArgs&gt; somethingHappened();
 *     <br/>
 *     // Simple method that can be called from the client.
 *     int calculate(int a, int b);
 * }
 * <br/>
 * public class HelloService implements IHello
 * {
 *     {@literal @}Override
 *     Event&lt;MyEventArgs&gt; somethingHappened()
 *     {
 *         return mySomethingHappenedEvent.getApi();
 *     }
 *     <br/>
 *     {@literal @}Override
 *     public int calculate(int a, int b)
 *     {
 *         return a + b;
 *     }
 *     <br/>
 *     // Helper method just to demonstrate how
 *     // to raise an event. 
 *     public void raiseEvent()
 *     {
 *         if (mySomethingHappenedEvent.isSubscribed())
 *         {
 *             MyEventArgs anEvent = new MyEventArgs();
 *             mySomethingHappenedEvent.raise(this, anEvent);
 *         }
 *     }
 *     <br/>
 *     private EventImpl&lt;MyEventArgs&gt; mySomethingHappenedEvent= new EventImpl&lt;MyEventArgs&gt;();
 * }
 * <br/>
 * public class Program
 * {
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Instantiating the service class.
 *         HelloService aHelloService = new HelloService();
 *         <br/>
 *         // Exposing the service via RPC.
 *         RpcFactory anRpcFactory = new RpcFactory();
 *         IRpcService&lt;IHello&gt; aService = anRpcFactory.createService(aHelloService, IHello.class);
 *         <br/>
 *         // Using TCP for the communication.
 *         IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *         IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8045/");
 *         <br/>
 *         // Attach input channel and start listening.
 *         aService.attachDuplexInputChannel(anInputChannel);
 *         <br/>
 *         System.out.println("Hello service is running. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *         <br/>
 *         // Detach input channel and stop listening.
 *         aService.detachDuplexInputChannel();
 *     }
 * }
 * </pre>
 * 
 * Calling the service from the client:
 * <pre>
 * public class Program
 * {
 *     // Event handler
 *     private static EventHandler&lt;MyEventArgs&gt; myOnSomethingHappened = new EventHandler&lt;MyEventArgs&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender, MyEventArgs e)
 *         {
 *             onSomethingHappened(sender, e);
 *         }
 *     };
 *     <br/>
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Create the client.
 *         IRpcFactory anRpcFactory = new RpcFactory();
 *         IRpcClient&lt;IHello&gt; aClient = anRpcFactory.createClient(IHello.class);
 *         <br/>
 *         // Use TCP.
 *         IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *         IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8045/");
 *         <br/>
 *         // Attach the output channel and connect the service.
 *         aClient.attachDuplexOutputChannel(anOutputChannel);
 *         <br/>
 *         <br/>
 *         // Get the service proxy.
 *         ICalculator aHelloProxy = aClient.getProxy();
 *         <br/>
 *         // Subscribe event in the service.
 *         aHelloProxy.somethingHappened().subscribe(myOnSomethingHappened);
 *         <br/>
 *         // Call method in the service.
 *         int aResult = aHelloProxy.calculate(10, 30);
 *         <br/>
 *         System.out.println("Result = " + Integer.toString(aResult));
 *         <br/>
 *         <br/>
 *         // Unsubscribe the event.
 *         aHelloProxy.somethingHappened().unsubscribe(myOnSomethingHappened);
 *         <br/>
 *         // Disconnect from the service.
 *         aClient.detachDuplexOutputChannel();
 *     }
 *     <br/>
 *     private static void onSomethingHappened(Object sender, EventArgs e)
 *     {
 *         // Handle the event from the service here.
 *     }
 * }
 * </pre>
 *  
 */
public class RpcFactory implements IRpcFactory
{
    /**
     * Constructs RpcFactory with default {@link XmlStringSerializer}.
     */
    public RpcFactory()
    {
        this(new XmlStringSerializer());
    }
    
    /**
     * Constructs RpcFactory with specified serializer.
     * @param serializer serializer to be used to serialize/deserialize communication between client and service.
     * Here is the list of serializers provided by Eneter: {@link eneter.messaging.dataprocessing.serializing}.
     */
    public RpcFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;

            // Default timeout is set to infinite by default.
            myRpcTimeout = 0;
            
            myRpcClientThreading = new SyncDispatching();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RpcClient<TServiceInterface>(mySerializer, myRpcTimeout, myRpcClientThreading.getDispatcher(), clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TServiceInterface> IRpcService<TServiceInterface> createService(TServiceInterface service, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RpcService<TServiceInterface>(service, mySerializer ,clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <TServiceInterface> IRpcService<TServiceInterface> createService(
            IFunction<TServiceInterface> serviceFactoryMethod,
            Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RpcService<TServiceInterface>(serviceFactoryMethod, mySerializer ,clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Returns serializer used to serialize/deserialize messages between client and service.
     * @return
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    /**
     * Sets serializer to be used to serialize/deserialize messages between client and service.
     * @param serializer
     * @return
     */
    public RpcFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    /**
     * Gets threading mechanism used for invoking events (if RPC interface has some) and ConnectionOpened and ConnectionClosed events.
     * @return thread dispatcher provider which returns thread dispatcher that is used to rout events.
     */
    public IThreadDispatcherProvider getRpcClientThreading()
    {
        return myRpcClientThreading;
    }
    
    /**
     * Sets threading mechanism used for invoking events (if RPC interface has some) and ConnectionOpened and ConnectionClosed events.
     * 
     * Default setting is that events are routed one by one via a working thread.<br/>
     * It is recomended not to set the same threading mode for the attached output channel because a deadlock can occur when
     * a remote procedure is called (e.g. if a return value from a remote method is routed to the same thread as is currently waiting for that return value the deadlock occurs).<br/>
     * <br/>
     * Note: The threading mode for the RPC service is defined by the threading mode of attached duplex input channel.
     * 
     * @param threading threading mode that shall be used for routing events.
     * @return
     */
    public RpcFactory setRpcClientThreading(IThreadDispatcherProvider threading)
    {
        myRpcClientThreading = threading;
        return this;
    }
    
    /**
     * Gets timeout which specifies until when a call to a remote method must return.
     * Default value is 0 what is the infinite time.
     * @return
     */
    public int getRpcTimeout()
    {
        return myRpcTimeout;
    }
    
    /**
     * Sets timeout which specifies until when a call to a remote method must return.
     * Default value is 0 what is the infinite time.
     * @param rpcTimeout
     * @return
     */
    public RpcFactory setRpcTimeout(int rpcTimeout)
    {
        myRpcTimeout = rpcTimeout;
        return this;
    }
    
    private ISerializer mySerializer;
    private IThreadDispatcherProvider myRpcClientThreading;
    private int myRpcTimeout;
}
