package eneter.messaging.endpoints.rpc;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.*;
import org.junit.experimental.categories.Categories.ExcludeCategory;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.net.system.*;
import eneter.net.system.internal.Cast;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.AutoResetEvent;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;


public abstract class RpcBaseTester
{
   
    public static interface IHello
    {
        Event<String> Open();
        Event<EventArgs> Close();
        
        int Sum(int a, int b);
        String CreateString(String src);
        String GetInstanceId() throws Exception;
        void Fail() throws IllegalStateException;
        void Timeout() throws TimeoutException;
    }
    
    public class HelloService implements IHello
    {
        public HelloService()
        {
            myInstanceId = UUID.randomUUID().toString();
        }

        @Override
        public Event<String> Open()
        {
            return myOpenEvent.getApi();
        }

        @Override
        public Event<EventArgs> Close()
        {
            return myCloseEvent.getApi();
        }

        @Override
        public int Sum(int a, int b)
        {
            return a + b;
        }

        @Override
        public String CreateString(String src)
        {
            return src;
        }
        
        @Override
        public String GetInstanceId() throws Exception
        {
            if (myOpenEvent.isSubscribed())
            {
                myOpenEvent.raise(this, myInstanceId);
            }
            
            return myInstanceId;
        }

        @Override
        public void Fail()
        {
            throw new IllegalStateException("My testing exception.");
        }

        @Override
        public void Timeout()
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (Exception err)
            {
            }
        }
        
        public void raiseOpen(String openArgs) throws Exception
        {
            if (myOpenEvent.isSubscribed())
            {
                myOpenEvent.raise(this, openArgs);
            }
        }

        public void raiseClose() throws Exception
        {
            if (myCloseEvent.isSubscribed())
            {
                myCloseEvent.raise(this, new EventArgs());
            }
        }
        
        
        private String myInstanceId;
        
        EventImpl<String> myOpenEvent = new EventImpl<String>();
        EventImpl<EventArgs> myCloseEvent = new EventImpl<EventArgs>();
    }
    
    private class EmptySerializer implements ISerializer
    {
        @Override
        public <T> Object serialize(T dataToSerialize, Class<T> clazz)
                throws Exception
        {
            return dataToSerialize;
        }

        @Override
        public <T> T deserialize(Object serializedData, Class<T> clazz)
                throws Exception
        {
            return Cast.as(serializedData, clazz);
        }
    }
    
    
    @Test
    public void rpcCall() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            IHello aServiceProxy = anRpcClient.getProxy();
            int k = aServiceProxy.Sum(1, 2);

            assertEquals(3, k);
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcCall_NullArgument() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            IHello aServiceProxy = anRpcClient.getProxy();
            String k = aServiceProxy.CreateString(null);

            assertEquals(null, k);
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void dynamicRpcCall() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            Object[] args = {1, 2};
            int k = (Integer) anRpcClient.callRemoteMethod("Sum", args);

            assertEquals(3, k);
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcCallError() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        String anExceptionType = null;
        String anExceptionMessage = null;
        String anExceptionDetails = null;
        
        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            IHello aServiceProxy = anRpcClient.getProxy();
            aServiceProxy.Fail();
        }
        catch (RpcException err)
        {
            anExceptionMessage = err.getMessage();
            anExceptionType = err.getServiceExceptionType();
            anExceptionDetails = err.getServiceExceptionDetails();
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
        
        assertEquals("IllegalStateException", anExceptionType);
        assertNotNull(anExceptionMessage);
        assertNotNull(anExceptionDetails);
    }
    
    // Note: This test is not applicable for the synchronous messaging
    //       because synchronous messaging is a sequence within one thread and so the remote call
    //       does not wait.
    @Test(expected = TimeoutException.class)
    public void rpcTimeout() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer)
            .setRpcTimeout(1000);


        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            IHello aServiceProxy = anRpcClient.getProxy();
            aServiceProxy.Timeout();
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcNonGenericEvent() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            IHello aServiceProxy = anRpcClient.getProxy();

            final AutoResetEvent anEventReceived = new AutoResetEvent(false);
            EventHandler<EventArgs> anEventHandler = new EventHandler<EventArgs>()
            {
                @Override
                public void onEvent(Object sender, EventArgs e)
                {
                    anEventReceived.set();
                }
            }; 

            // Subscribe.
            aServiceProxy.Close().subscribe(anEventHandler);

            // Raise the event in the service.
            aService.raiseClose();

            anEventReceived.waitOne();

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.raiseClose();

            assertFalse(anEventReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void dynamicRpcNonGenericEvent() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            final AutoResetEvent aCloseReceived = new AutoResetEvent(false);
            EventHandler<EventArgs> aCloseHandler = new EventHandler<EventArgs>()
            {
                @Override
                public void onEvent(Object sender, EventArgs e)
                {
                    aCloseReceived.set();
                }
            };
            
            // Subscribe.
            anRpcClient.subscribeRemoteEvent("Close", aCloseHandler);

            // Raise the event in the service.
            aService.raiseClose();

            aCloseReceived.waitOne();

            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Close", aCloseHandler);
            
            // Try to raise again.
            aService.raiseClose();

            assertFalse(aCloseReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcGenericEvent() throws Exception
    {

        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            IHello aServiceProxy = anRpcClient.getProxy();

            final AutoResetEvent anEventReceived = new AutoResetEvent(false);
            EventHandler<String> anEventHandler = new EventHandler<String>()
            {
                @Override
                public void onEvent(Object sender, String e)
                {
                    anEventReceived.set();
                }
            }; 

            // Subscribe.
            aServiceProxy.Open().subscribe(anEventHandler);

            // Raise the event in the service.
            String anOpenArgs = "Hello";
            aService.raiseOpen(anOpenArgs);

            anEventReceived.waitOne();

            // Unsubscribe.
            aServiceProxy.Open().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.raiseOpen(anOpenArgs);

            assertFalse(anEventReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void dynamicRpcGenericEvent() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            final AutoResetEvent anOpenReceived = new AutoResetEvent(false);
            final String[] aReceivedOpenData = { null };  
            EventHandler<String> anOpenHandler = new EventHandler<String>()
            {
                @Override
                public void onEvent(Object sender, String e)
                {
                    aReceivedOpenData[0] = e;
                    anOpenReceived.set();
                }
            };

            // Subscribe.
            anRpcClient.subscribeRemoteEvent("Open", anOpenHandler);

            // Raise the event in the service.
            aService.raiseOpen("Hello");

            anOpenReceived.waitOne();

            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Open", anOpenHandler);
            
            assertEquals("Hello", aReceivedOpenData[0]);

            // Try to raise again.
            aService.raiseClose();
            aService.raiseOpen("Hello2");

            assertFalse(anOpenReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void subscribeBeforeAttachOutputChannel() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            IHello aServiceProxy = anRpcClient.getProxy();

            final AutoResetEvent aClientConnected = new AutoResetEvent(false);
            anRpcClient.connectionOpened().subscribe(new EventHandler<DuplexChannelEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelEventArgs e)
                {
                    aClientConnected.set();
                }
            });

            final AutoResetEvent anEventReceived = new AutoResetEvent(false);
            EventHandler<EventArgs> anEventHandler = new EventHandler<EventArgs>()
            {
                @Override
                public void onEvent(Object sender, EventArgs e)
                {
                    anEventReceived.set();
                }
            };

            // Subscribe before the connection is open.
            aServiceProxy.Close().subscribe(anEventHandler);

            // Open the connection.
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            // Wait until the client is connected.
            aClientConnected.waitOne();

            // Raise the event in the service.
            aService.raiseClose();

            anEventReceived.waitOne();

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.raiseClose();

            assertFalse(anEventReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcNonGenericEvent_10000() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            IHello aServiceProxy = anRpcClient.getProxy();

            final int[] aCounter = { 0 };
            final AutoResetEvent anEventReceived = new AutoResetEvent(false);
            EventHandler<EventArgs> anEventHandler = new EventHandler<EventArgs>()
            {
                @Override
                public void onEvent(Object sender, EventArgs e)
                {
                    ++aCounter[0];
                    if (aCounter[0] == 10000)
                    {
                        anEventReceived.set();
                    }
                }
            }; 

            // Subscribe.
            aServiceProxy.Close().subscribe(anEventHandler);

            long aStartTime = System.currentTimeMillis();
            
            // Raise the event in the service.
            for (int i = 0; i < 10000; ++i)
            {
                aService.raiseClose();
            }

            anEventReceived.waitOne();
            
            long aDeltaTime1 = System.currentTimeMillis() - aStartTime;
            System.out.println("Elapsed time: " + Long.toString(aDeltaTime1));

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.raiseClose();
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void dynamicRpcNonGenericEvent_10000() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        HelloService aService = new HelloService();
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            final int[] aCounter = { 0 };
            final AutoResetEvent aCloseReceived = new AutoResetEvent(false);
            EventHandler<EventArgs> aCloseHandler = new EventHandler<EventArgs>()
            {
                @Override
                public void onEvent(Object sender, EventArgs e)
                {
                    ++aCounter[0];
                    if (aCounter[0] == 10000)
                    {
                        aCloseReceived.set();
                    }
                }
            };
            
            // Subscribe.
            anRpcClient.subscribeRemoteEvent("Close", aCloseHandler);

            long aStartTime = System.currentTimeMillis();
            
            // Raise the event in the service.
            for (int i = 0; i < 10000; ++i)
            {
                aService.raiseClose();
            }

            aCloseReceived.waitOne();
            
            long aDeltaTime1 = System.currentTimeMillis() - aStartTime;
            System.out.println("Elapsed time: " + Long.toString(aDeltaTime1));
            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Close", aCloseHandler);
            
            // Try to raise again.
            aService.raiseClose();

            assertFalse(aCloseReceived.waitOne(1000));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void rpcCall_10000() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            IHello aServiceProxy = anRpcClient.getProxy();
            
            long aStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < 10000; ++i)
            {
                aServiceProxy.Sum(1, 2);
            }
            
            long aDeltaTime1 = System.currentTimeMillis() - aStartTime;
            System.out.println("Rpc call. Elapsed time: " + Long.toString(aDeltaTime1));

            HelloService aService = new HelloService();

            long aStartTime2 = System.currentTimeMillis();

            for (int i = 0; i < 10000; ++i)
            {
                aService.Sum(1, 2);
            }

            long aDeltaTime2 = System.currentTimeMillis() - aStartTime2;
            System.out.println("Local call. Elapsed time: " + Long.toString(aDeltaTime2));
        }
        finally
        {
            if (anRpcClient.isDuplexOutputChannelAttached())
            {
                anRpcClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    public void multipleClients_RemoteCall_10() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.StartProfiler();

        RpcFactory anRpcFactory = new RpcFactory(mySerializer).setRpcTimeout(30);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(new HelloService(), IHello.class);

        final ArrayList<IRpcClient<IHello>> aClients = new ArrayList<IRpcClient<IHello>>();
        for (int i = 0; i < 10; ++i )
        {
            aClients.add(anRpcFactory.createClient(IHello.class));
        }

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));

            // Clients open connection.
            for(IRpcClient<IHello> aClient : aClients)
            {
                aClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));
            }

            // Clients communicate with the service in parallel.
            final AutoResetEvent aDone = new AutoResetEvent(false);
            final int[] aCounter = { 0 };
            for (final IRpcClient<IHello> aClient : aClients)
            {
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            aClient.getProxy().Sum(10, 20);
                            ++aCounter[0];
                            if (aCounter[0] == aClients.size())
                            {
                                aDone.set();
                            }
                            Thread.sleep(1);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error("Detected Exception.", err);
                            aDone.set();
                        }
                    }
                });
            }

            aDone.waitOne();

            //EneterTrace.StopProfiler();

            assertEquals(aClients.size(), aCounter[0]);
        }
        finally
        {
            for (IRpcClient<IHello> aClient : aClients)
            {
                aClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test
    public void multipleClients_RemoteEvent_10() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.StartProfiler();

        HelloService aService = new HelloService();
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createSingleInstanceService(aService, IHello.class);

        final ArrayList<IRpcClient<IHello>> aClients = new ArrayList<IRpcClient<IHello>>();
        for (int i = 0; i < 10; ++i)
        {
            aClients.add(anRpcFactory.createClient(IHello.class));
        }

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));

            // Clients open connection.
            for (IRpcClient<IHello> aClient : aClients)
            {
                aClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));
            }

            // Subscribe to remote event from the service.
            final AutoResetEvent anOpenReceived = new AutoResetEvent(false);
            final AutoResetEvent aCloseReceived = new AutoResetEvent(false);
            final AutoResetEvent anAllCleintsSubscribed = new AutoResetEvent(false);
            final int[] anOpenCounter = { 0 };
            final int[] aCloseCounter = { 0 };
            final int[] aSubscribedClientCounter = { 0 };
            for (IRpcClient<IHello> aClient : aClients)
            {
                final IRpcClient<IHello> aClientTmp = aClient;
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        aClientTmp.getProxy().Open().subscribe(new EventHandler<String>()
                        {
                            @Override
                            public void onEvent(Object sender, String e)
                            {
                                ++anOpenCounter[0];
                                if (anOpenCounter[0] == aClients.size())
                                {
                                    anOpenReceived.set();
                                }
                            }
                        });
                        
                        aClientTmp.getProxy().Close().subscribe(new EventHandler<EventArgs>()
                        {
                            @Override
                            public void onEvent(Object sender, EventArgs e)
                            {
                                ++aCloseCounter[0];
                                if (aCloseCounter[0] == aClients.size())
                                {
                                    aCloseReceived.set();
                                }
                            }
                        });

                        ++aSubscribedClientCounter[0];
                        if (aSubscribedClientCounter[0] == aClients.size())
                        {
                            anAllCleintsSubscribed.set();
                        }
    
                        try
                        {
                            Thread.sleep(1);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error("Thread.sleep() failed.");
                        }
                    }
                });
            }

            // Wait until all clients are subscribed.
            anAllCleintsSubscribed.waitOne();

            // Servicde raises two different events.
            String anOpenArgs = "Hello";
            aService.raiseOpen(anOpenArgs);
            aService.raiseClose();


            anOpenReceived.waitOne();
            aCloseReceived.waitOne();

        }
        finally
        {
            for (IRpcClient<IHello> aClient : aClients)
            {
                aClient.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    @Test(expected = IllegalStateException.class)
    public void noInterfaceTypeProvided()
    {
        RpcFactory anRpcFactory = new RpcFactory();
        anRpcFactory.createClient(String.class);
    }
    
    @Test
    public void PerClientInstanceService() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createPerClientInstanceService(new IFunction<IHello>()
        {
            @Override
            public IHello invoke() throws Exception
            {
                return new HelloService();
            }
        }, IHello.class);
    
        
        IRpcClient<IHello> anRpcClient1 = anRpcFactory.createClient(IHello.class);
        IRpcClient<IHello> anRpcClient2 = anRpcFactory.createClient(IHello.class);

        try
        {
            final ManualResetEvent anEvent1Received = new ManualResetEvent(false);
            final String[] aService1IdFromEvent = { null };
            anRpcClient1.getProxy().Open().subscribe(new EventHandler<String>()
            {
                @Override
                public void onEvent(Object sender, String e)
                {
                    aService1IdFromEvent[0] = e;
                    anEvent1Received.set();
                }
            });

            final ManualResetEvent anEvent2Received = new ManualResetEvent(false);
            final String[]  aService2IdFromEvent = { null };
            anRpcClient2.getProxy().Open().subscribe(new EventHandler<String>()
            {
                @Override
                public void onEvent(Object sender, String e)
                {
                    aService2IdFromEvent[0] = e;
                    anEvent2Received.set();
                }
            });

            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient1.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));
            anRpcClient2.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));

            // Note: open connection runs async, because it subscribes events at service and the thread cannot be blocked.
            //       So provide some time the connection can be open.
            Thread.sleep(200);

            EneterTrace.debug("Invoking starts.");
            String aServiceId1 = anRpcClient1.getProxy().GetInstanceId();
            String aServiceId2 = anRpcClient2.getProxy().GetInstanceId();
            
            anEvent1Received.waitOne();
            anEvent2Received.waitOne();

            assertFalse(StringExt.isNullOrEmpty(aServiceId1));
            assertFalse(StringExt.isNullOrEmpty(aService1IdFromEvent[0]));
            assertFalse(StringExt.isNullOrEmpty(aServiceId2));
            assertFalse(StringExt.isNullOrEmpty(aService2IdFromEvent[0]));
            assertEquals(aServiceId1, aService1IdFromEvent[0]);
            assertEquals(aServiceId2, aService2IdFromEvent[0]);
            assertThat(aServiceId1, not(equalTo(aServiceId2)));
        }
        finally
        {
            if (anRpcClient1.isDuplexOutputChannelAttached())
            {
                anRpcClient1.detachDuplexOutputChannel();
            }
            if (anRpcClient2.isDuplexOutputChannelAttached())
            {
                anRpcClient2.detachDuplexOutputChannel();
            }

            if (anRpcService.isDuplexInputChannelAttached())
            {
                anRpcService.detachDuplexInputChannel();
            }
        }
    }
    
    
    protected IMessagingSystemFactory myMessaging;
    protected String myChannelId;
    protected ISerializer mySerializer;
}
