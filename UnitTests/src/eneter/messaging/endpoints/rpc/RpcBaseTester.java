package eneter.messaging.endpoints.rpc;

import static org.junit.Assert.*;

import java.util.concurrent.TimeoutException;

import org.junit.*;
import org.junit.experimental.categories.Categories.ExcludeCategory;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.net.system.*;
import eneter.net.system.internal.Cast;
import eneter.net.system.threading.internal.AutoResetEvent;


public abstract class RpcBaseTester
{
   
    public static interface IHello
    {
        Event<String> Open();
        Event<EventArgs> Close();
        
        int Sum(int a, int b);
        String CreateString(String src);
        void Fail() throws IllegalStateException;
        void Timeout() throws TimeoutException;
    }
    
    public class HelloService implements IHello
    {

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

        public void RaiseClose() throws Exception
        {
            if (myCloseEvent.isSubscribed())
            {
                myCloseEvent.raise(this, new EventArgs());
            }
        }
        
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
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
    
    @Test(expected = IllegalStateException.class)
    public void rpcCallError() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer);
        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
        IRpcClient<IHello> anRpcClient = anRpcFactory.createClient(IHello.class);

        try
        {
            anRpcService.attachDuplexInputChannel(myMessaging.createDuplexInputChannel(myChannelId));
            anRpcClient.attachDuplexOutputChannel(myMessaging.createDuplexOutputChannel(myChannelId));


            IHello aServiceProxy = anRpcClient.getProxy();
            aServiceProxy.Fail();
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
    
    // Note: This test is not applicable for the synchronous messaging
    //       because synchronous messaging is a sequence within one thread and so the remote call
    //       does not wait.
    @Test(expected = TimeoutException.class)
    public void rpcTimeout() throws Exception
    {
        RpcFactory anRpcFactory = new RpcFactory(mySerializer)
            .setRpcTimeout(1000);


        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
            aService.RaiseClose();

            anEventReceived.waitOne();

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.RaiseClose();

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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
            aService.RaiseClose();

            aCloseReceived.waitOne();

            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Close", aCloseHandler);
            
            // Try to raise again.
            aService.RaiseClose();

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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
            aService.RaiseClose();
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
            aService.RaiseClose();

            anEventReceived.waitOne();

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.RaiseClose();

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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
                aService.RaiseClose();
            }

            anEventReceived.waitOne();
            
            long aDeltaTime1 = System.currentTimeMillis() - aStartTime;
            System.out.println("Elapsed time: " + Long.toString(aDeltaTime1));

            // Unsubscribe.
            aServiceProxy.Close().unsubscribe(anEventHandler);

            // Try to raise again.
            aService.RaiseClose();
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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(aService, IHello.class);
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
                aService.RaiseClose();
            }

            aCloseReceived.waitOne();
            
            long aDeltaTime1 = System.currentTimeMillis() - aStartTime;
            System.out.println("Elapsed time: " + Long.toString(aDeltaTime1));
            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Close", aCloseHandler);
            
            // Try to raise again.
            aService.RaiseClose();

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
        IRpcService<IHello> anRpcService = anRpcFactory.createService(new HelloService(), IHello.class);
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
    
    
    protected IMessagingSystemFactory myMessaging;
    protected String myChannelId;
    protected ISerializer mySerializer;
}
