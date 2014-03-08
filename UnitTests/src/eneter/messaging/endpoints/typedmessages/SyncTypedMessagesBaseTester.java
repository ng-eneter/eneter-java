package eneter.messaging.endpoints.typedmessages;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.SyncDispatching;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

public abstract class SyncTypedMessagesBaseTester
{
    @Test
    public void syncRequestResponse() throws Exception
    {
        final IDuplexTypedMessageReceiver<String, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(String.class, Integer.class);
        aReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                int aResult = y.getRequestMessage() * 10;
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(aResult));
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response message failed.", err);
                }
            }
        });
        
        ISyncDuplexTypedMessageSender<String, Integer> aSender = DuplexTypedMessagesFactory.createSyncDuplexTypedMessageSender(String.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            String aResult = aSender.sendRequestMessage(100);

            assertEquals("1000", aResult);

        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }
    
    
    @Test(expected = IllegalStateException.class)
    public void connectionClosedDuringWaitingForResponse() throws Exception
    {
        final IDuplexTypedMessageReceiver<Integer, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(Integer.class, Integer.class);
        aReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                // Disconnect the cient.
                try
                {
                    aReceiver.getAttachedDuplexInputChannel().disconnectResponseReceiver(y.getResponseReceiverId());
                }
                catch (Exception err)
                {
                    EneterTrace.error("Disconnection of the client failed.", err);
                }
            }
        });

        ISyncDuplexTypedMessageSender<Integer, Integer> aSender = DuplexTypedMessagesFactory.createSyncDuplexTypedMessageSender(Integer.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            aSender.sendRequestMessage(100);

            // The previous call should raise the exception there we failed if we are here.
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }
    
    @Test
    public void detachOutputChannelDuringWaitingForResponse() throws Exception
    {
        IDuplexTypedMessageReceiver<Integer, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(Integer.class, Integer.class);

        final ISyncDuplexTypedMessageSender<Integer, Integer> aSender = DuplexTypedMessagesFactory.createSyncDuplexTypedMessageSender(Integer.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            // Send the request from a different thread.
            final AutoResetEvent aWaitingInterrupted = new AutoResetEvent(false);
            final Exception[] aCaughtException = {null};
            
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        aSender.sendRequestMessage(100);
                    }
                    catch (Exception err)
                    {
                        aCaughtException[0] = err;
                    }

                    aWaitingInterrupted.set();
                }
            });
            
            Thread.sleep(100);

            // Detach the output channel while other thread waits for the response.
            aSender.detachDuplexOutputChannel();

            assertTrue(aWaitingInterrupted.waitOne(50000));

            assertTrue(aCaughtException != null && (aCaughtException[0] instanceof IllegalStateException));
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }
    
    @Test(expected = IllegalStateException.class)
    public void waitingForResponseTimeouted() throws Exception
    {
        IDuplexTypedMessageReceiver<Integer, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(Integer.class, Integer.class);

        // Create sender expecting the response within 500 ms.
        IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory(500);
        ISyncDuplexTypedMessageSender<Integer, Integer> aSender = aSenderFactory.createSyncDuplexTypedMessageSender(Integer.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            // This call should throw exception.
            aSender.sendRequestMessage(100);
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }
    
    @Test
    public void threadDispatching() throws Exception
    {
        final IDuplexTypedMessageReceiver<String, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(String.class, Integer.class);
        aReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                int aResult = y.getRequestMessage() * 10;
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(aResult));
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response message failed.", err);
                }
            }
        });

        // Set windows working thread dispatcher.
        SyncDispatching aSyncDispatcher = new SyncDispatching(true);
        
        final ManualResetEvent aThreadIdStored = new ManualResetEvent(false);
        final long[] aWorkingThreadIs = { 0 };
        aSyncDispatcher.getDispatcher().invoke(new Runnable()
        {
            @Override
            public void run()
            {
                aWorkingThreadIs[0] = Thread.currentThread().getId();
                aThreadIdStored.set();
            }
        });
        aThreadIdStored.waitOne();
        
        ((DuplexTypedMessagesFactory)DuplexTypedMessagesFactory).setSyncDuplexTypedSenderThreadMode(aSyncDispatcher);

        ISyncDuplexTypedMessageSender<String, Integer> aSender = DuplexTypedMessagesFactory.createSyncDuplexTypedMessageSender(String.class, Integer.class);

        final long[] aOpenConnectionThreadId = { 0 };
        aSender.connectionOpened().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelEventArgs e)
            {
                aOpenConnectionThreadId[0] = Thread.currentThread().getId();
            }
        });
        
        final ManualResetEvent aConnectionClosedEvent = new ManualResetEvent(false);
        final long[] aCloseConnectionThreadId = { 0 };
        aSender.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelEventArgs e)
            {
                aCloseConnectionThreadId[0] = Thread.currentThread().getId();
                aConnectionClosedEvent.set();
            }
        });
        
        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            String aResult = aSender.sendRequestMessage(100);

            aReceiver.getAttachedDuplexInputChannel().disconnectResponseReceiver(aSender.getAttachedDuplexOutputChannel().getResponseReceiverId());
            aConnectionClosedEvent.waitOne();

            assertEquals("1000", aResult);
            assertEquals(aWorkingThreadIs[0], aOpenConnectionThreadId[0]);
            assertEquals(aWorkingThreadIs[0], aCloseConnectionThreadId[0]);
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }
    
    protected IDuplexInputChannel InputChannel;
    protected IDuplexOutputChannel OutputChannel;
    protected ISerializer Serializer;
    protected IDuplexTypedMessagesFactory DuplexTypedMessagesFactory;
}
