package eneter.messaging.endpoints.typedmessages;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.AutoResetEvent;

public abstract class TypedRequestResponseBaseTester
{
    protected void setup(IMessagingSystemFactory messagingSystemFactory, String channelId, ISerializer serializer) throws Exception
    {
        MessagingSystemFactory = messagingSystemFactory;

        DuplexOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(channelId);
        DuplexInputChannel = MessagingSystemFactory.createDuplexInputChannel(channelId);

        IDuplexTypedMessagesFactory aMessageFactory = new DuplexTypedMessagesFactory(serializer);
        Requester = aMessageFactory.createDuplexTypedMessageSender(Integer.class, Integer.class);
        Responser = aMessageFactory.createDuplexTypedMessageReceiver(Integer.class, Integer.class);
    }

    @Test
    public void sendReceive_1Message() throws Exception
    {
        // The test can be performed from more thread therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final int[] aReceivedMessage = {0};
        Responser.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                aReceivedMessage[0] = y.getRequestMessage();

                // Send the response
                try
                {
					Responser.sendResponseMessage(y.getResponseReceiverId(), 1000);
				}
                catch (Exception err)
                {
					EneterTrace.error("Sending of response message failed.", err);
				}
            }
        });
        Responser.attachDuplexInputChannel(DuplexInputChannel);

        final int[] aReceivedResponse = {0};
        Requester.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<Integer> y)
            {
                aReceivedResponse[0] = y.getResponseMessage();

                // Signal that the response message was received -> the loop is closed.
                aMessageReceivedEvent.set();
            }
        });
        Requester.attachDuplexOutputChannel(DuplexOutputChannel);

        try
        {
            Requester.sendRequestMessage(2000);

            // Wait for the signal that the message is received.
            assertTrue(aMessageReceivedEvent.waitOne(2000));
        }
        finally
        {
            Requester.detachDuplexOutputChannel();
            Responser.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(2000, aReceivedMessage[0]);
        assertEquals(1000, aReceivedResponse[0]);
    }
    
    @Test
    public void sendReceive_MultiThreadAccess_1000Messages()
        throws Exception
    {
        // The test can be performed from more thread therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        Responser.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add(y.getRequestMessage());
                }

                // Send the response
                try
                {
					Responser.sendResponseMessage(y.getResponseReceiverId(), 1000);
				}
                catch (Exception err)
                {
					EneterTrace.error("Sending of response message failed.", err);
				}
            }
        });
        Responser.attachDuplexInputChannel(DuplexInputChannel);

        final ArrayList<Integer> aReceivedResponses = new ArrayList<Integer>();
        Requester.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<Integer> y)
            {
                synchronized (aReceivedResponses)
                {
                    aReceivedResponses.add(y.getResponseMessage());

                    if (aReceivedResponses.size() == 1000)
                    {
                        // Signal that the message was received.
                        aMessageReceivedEvent.set();
                    }
                }
            }
        });
        Requester.attachDuplexOutputChannel(DuplexOutputChannel);

        try
        {
            ArrayList<Thread> aThreads = new ArrayList<Thread>();

            for (int i = 0; i < 10; ++i)
            {
                Thread aThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (int ii = 0; ii < 100; ++ii)
                        {
                            try
                            {
                                Requester.sendRequestMessage(2000);
                                Thread.sleep(1);
                            }
                            catch (Exception err)
                            {
                            }
                        }
                    }
                });

                aThreads.add(aThread);
            }

            for (Thread x : aThreads)
            {
                x.start();
            }

            // Wait for the signal that the message is received.
            assertTrue(aMessageReceivedEvent.waitOne(10000));
        }
        finally
        {
            Requester.detachDuplexOutputChannel();
            Responser.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessages.size());
        for (int x : aReceivedMessages)
        {
            assertEquals(2000, x);
        }

        assertEquals(1000, aReceivedResponses.size());
        for (int x : aReceivedResponses)
        {
            assertEquals(1000, x);
        }
    }
    
    
    protected IMessagingSystemFactory MessagingSystemFactory;
    protected IDuplexOutputChannel DuplexOutputChannel;
    protected IDuplexInputChannel DuplexInputChannel;

    protected IDuplexTypedMessageSender<Integer, Integer> Requester;
    protected IDuplexTypedMessageReceiver<Integer, Integer> Responser;
}
