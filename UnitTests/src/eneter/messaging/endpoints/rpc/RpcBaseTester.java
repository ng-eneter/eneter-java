package eneter.messaging.endpoints.rpc;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
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
        void Fail();
        void Timeout();
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
            anRpcClient.subscribeRemoteEvent("Close", anEventHandler);

            // Raise the event in the service.
            aService.RaiseClose();

            anEventReceived.waitOne();

            // Unsubscribe.
            anRpcClient.unsubscribeRemoteEvent("Close", anEventHandler);

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
    
    
    protected IMessagingSystemFactory myMessaging;
    protected String myChannelId;
    protected ISerializer mySerializer;
}
