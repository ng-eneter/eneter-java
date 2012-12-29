package eneter.messaging.endpoints.typedmessages;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;
import org.junit.experimental.categories.Categories.ExcludeCategory;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.IFunction1;
import eneter.net.system.threading.internal.AutoResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

public abstract class SyncTypedMessagesBaseTester
{
    @Test
    public void syncRequestResponse() throws Exception
    {
        ISyncTypedMessageReceiver<String, Integer> aReceiver = SyncTypedMessagesFactory.createSyncMessageReceiver(new IFunction1<String, TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public String invoke(TypedRequestReceivedEventArgs<Integer> x)
            {
                int aValue = x.getRequestMessage() * 10;
                return Integer.toString(aValue);
            }
        }, String.class, Integer.class);
                

        ISyncTypedMessageSender<String, Integer> aSender = SyncTypedMessagesFactory.createSyncMessageSender(String.class, Integer.class);

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
    
    @Test
    public void syncRequestAsyncResponse() throws Exception
    {
        final IDuplexTypedMessageReceiver<Integer, Integer> aReceiver = DuplexTypedMessagesFactory.createDuplexTypedMessageReceiver(Integer.class, Integer.class);
        aReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), y.getRequestMessage() * 10);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response message failed.", err);
                }
            }
        });

        ISyncTypedMessageSender<Integer, Integer> aSender = SyncTypedMessagesFactory.createSyncMessageSender(Integer.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            int aResult = aSender.sendRequestMessage(100);

            assertEquals(1000, aResult);

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

        ISyncTypedMessageSender<Integer, Integer> aSender = SyncTypedMessagesFactory.createSyncMessageSender(Integer.class, Integer.class);

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

        final ISyncTypedMessageSender<Integer, Integer> aSender = SyncTypedMessagesFactory.createSyncMessageSender(Integer.class, Integer.class);

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
        ISyncTypedMessagesFactory aSenderFactory = new SyncTypedMessagesFactory(500);
        ISyncTypedMessageSender<Integer, Integer> aSender = aSenderFactory.createSyncMessageSender(Integer.class, Integer.class);

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            // This call shoul throw exception.
            aSender.sendRequestMessage(100);
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
    protected ISyncTypedMessagesFactory SyncTypedMessagesFactory;
    protected IDuplexTypedMessagesFactory DuplexTypedMessagesFactory;
}
