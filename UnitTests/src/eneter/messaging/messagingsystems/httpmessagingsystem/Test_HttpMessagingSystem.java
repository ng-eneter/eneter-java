package eneter.messaging.messagingsystems.httpmessagingsystem;

import static org.junit.Assert.*;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.*;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.*;

public class Test_HttpMessagingSystem extends MessagingSystemBaseTester
{
    private class TConnectionEvent
    {
        public TConnectionEvent(long time, String receiverId)
        {
            Time = time;
            ReceiverId = receiverId;
        }

        public long Time;
        public String ReceiverId;
    }
    
    
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new HttpMessagingSystemFactory();
        myChannelId = "http://127.0.0.1:" + Integer.toString(aPort) + "/Testing/";
        
        myChannelId2 = "http://127.0.0.1:" + Integer.toString(aPort) + "/Testing2/";
        myChannelId3 = "http://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
    
    @Test(expected = ConnectException.class)
    @Override
    public void Oneway_06_StopListening()
        throws Exception
    {
        super.Oneway_06_StopListening();
    }
    
    @Test
    public void B01_InactivityTimeout() throws Exception
    {
        // Set the pulling frequency time (duplex output channel pulls for responses) higher
        // than inactivity timeout in the duplex input channel.
        // Therefore the timeout should occur before the pulling - this is how the
        // inactivity is simulated in this test.
        IMessagingSystemFactory aMessagingSystem = new HttpMessagingSystemFactory(3000, 2000);

        IDuplexOutputChannel anOutputChannel1 = aMessagingSystem.createDuplexOutputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel2 = aMessagingSystem.createDuplexOutputChannel(myChannelId);

        IDuplexInputChannel anInputChannel = aMessagingSystem.createDuplexInputChannel(myChannelId);

        final AutoResetEvent aConncetionEvent = new AutoResetEvent(false);
        final AutoResetEvent aDisconncetionEvent = new AutoResetEvent(false);

        final ArrayList<TConnectionEvent> aConnections = new ArrayList<TConnectionEvent>();
        anInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aConnections.add(new TConnectionEvent(System.currentTimeMillis(), y.getResponseReceiverId()));
                aConncetionEvent.set();
            }
        }); 

        final ArrayList<TConnectionEvent> aDisconnections = new ArrayList<TConnectionEvent>();
        anInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aDisconnections.add(new TConnectionEvent(System.currentTimeMillis(), y.getResponseReceiverId()));
                aDisconncetionEvent.set();
            }
        });

        try
        {
            anInputChannel.startListening();
            assertTrue(anInputChannel.isListening());

            // Create the 1st connection.
            anOutputChannel1.openConnection();
            assertTrue(anOutputChannel1.isConnected());
            aConncetionEvent.waitOne();
            aConncetionEvent.reset();
            assertEquals(1, aConnections.size());
            assertFalse(StringExt.isNullOrEmpty(aConnections.get(0).ReceiverId));

            Thread.sleep(1000);

            // Create the 2nd connection.
            anOutputChannel2.openConnection();
            assertTrue(anOutputChannel2.isConnected());
            aConncetionEvent.waitOne();
            assertEquals(2, aConnections.size());
            assertFalse(StringExt.isNullOrEmpty(aConnections.get(1).ReceiverId));

            // Wait for the 1st disconnection
            aDisconncetionEvent.waitOne();
            aDisconncetionEvent.reset();

            assertEquals(1, aDisconnections.size());
            assertEquals(aConnections.get(0).ReceiverId, aDisconnections.get(0).ReceiverId);
            long anElapsedTime = aDisconnections.get(0).Time - aConnections.get(0).Time;
            // Note: the elapsed time should be 2000ms but the timer does not have to be precise.
            assertTrue(anElapsedTime > 1900); 

            // Wait for the 2nd disconnection
            aDisconncetionEvent.waitOne();

            assertEquals(2, aDisconnections.size());
            assertEquals(aConnections.get(1).ReceiverId, aDisconnections.get(1).ReceiverId);
            assertTrue(aDisconnections.get(1).Time - aConnections.get(1).Time > 1900);
        }
        finally
        {
            anOutputChannel1.closeConnection();
            anOutputChannel2.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test
    public void B02_MoreListenersBasedOnSameTCP() throws Exception
    {
        ExecutorService aThreadPool = Executors.newFixedThreadPool(10);
        
        Future<?> anEnd1 = aThreadPool.submit(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    sendMessageReceiveResponse(myChannelId, "Hello1", "Response1", 1, 100);
                    return null;
                }
            });
        
        Future<?> anEnd2 = aThreadPool.submit(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    // One-way channel on the same TCP as duplex channel.
                    sendMessageViaOutputChannel(myChannelId2, "Hello2", 1, 100);
                    return null;
                }
            });
        
        Future<?> anEnd3 = aThreadPool.submit(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    sendMessageReceiveResponse(myChannelId3, "Hello3", "Response3", 1, 100);
                    return null;
                }
            });
        
        // Wait until completed.
        // Note: This will throw exception if some of these tests fails.
        anEnd1.get();
        anEnd2.get();
        anEnd3.get();
    }
    
    
    private String myChannelId2;
    private String myChannelId3;
}
