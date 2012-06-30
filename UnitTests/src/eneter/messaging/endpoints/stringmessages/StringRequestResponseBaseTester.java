package eneter.messaging.endpoints.stringmessages;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

public abstract class StringRequestResponseBaseTester
{
    protected void setup(IMessagingSystemFactory messagingSystemFactory, String channelId) throws Exception
    {
        myMessagingSystemFactory = messagingSystemFactory;

        myDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(channelId);
        myDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(channelId);

        IDuplexStringMessagesFactory aMessageFactory = new DuplexStringMessagesFactory();
        myMessageRequester = aMessageFactory.createDuplexStringMessageSender();
        myMessageResponser = aMessageFactory.createDuplexStringMessageReceiver();
    }
    
    @Test
    public void sendReceive_1Message() throws Exception
    {
        // The test can be performed from more thread therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final String[] aReceivedMessage = {""};
        final String[] aReceivedResponse = {""};

        try
        {
            myMessageResponser.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
            {
                @Override
                public void onEvent(Object x, StringRequestReceivedEventArgs y)
                {
                    aReceivedMessage[0] = y.getRequestMessage();

                    // Send the response
                    try
                    {
						myMessageResponser.sendResponseMessage(y.getResponseReceiverId(), "Response");
					}
                    catch (Exception err)
                    {
						EneterTrace.error("Sending of response message failed.", err);
					}
                }
            }); 
            myMessageResponser.attachDuplexInputChannel(myDuplexInputChannel);

            
            myMessageRequester.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
            {
                @Override
                public void onEvent(Object x, StringResponseReceivedEventArgs y)
                {
                    aReceivedResponse[0] = y.getResponseMessage();

                    // Signal that the response message was received -> the loop is closed.
                    aMessageReceivedEvent.set();
                }
            });
            myMessageRequester.attachDuplexOutputChannel(myDuplexOutputChannel);


            myMessageRequester.sendMessage("Message");

            // Wait for the signal that the message is received.
            assertTrue(aMessageReceivedEvent.waitOne(200));
        }
        finally
        {
            myMessageRequester.detachDuplexOutputChannel();
            myMessageResponser.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals("Message", aReceivedMessage[0]);
        assertEquals("Response", aReceivedResponse[0]);
    }
    
    @Test
    public void sendReceive_MultiThreadAccess_1000Messages() throws Exception
    {
        // The test can be performed from more thread therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final ArrayList<String> aReceivedMessages = new ArrayList<String>();
        myMessageResponser.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add(y.getRequestMessage());
                }

                // Send the response
                try 
                {
					myMessageResponser.sendResponseMessage(y.getResponseReceiverId(), "Response");
				}
                catch (Exception err)
                {
                	EneterTrace.error("Sending of response message failed.", err);
				}
            }
        });
        myMessageResponser.attachDuplexInputChannel(myDuplexInputChannel);

        final ArrayList<String> aReceivedResponses = new ArrayList<String>();
        myMessageRequester.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
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
        myMessageRequester.attachDuplexOutputChannel(myDuplexOutputChannel);


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
                            myMessageRequester.sendMessage("Message");
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

            for (Thread t : aThreads)
            {
                t.start();
            }

            // Wait for the signal that the message is received.
            //Assert.IsTrue(aMessageReceivedEvent.WaitOne(1000));
            aMessageReceivedEvent.waitOne();
        }
        finally
        {
            myMessageRequester.detachDuplexOutputChannel();
            myMessageResponser.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessages.size());
        for (String x : aReceivedMessages)
        {
            assertEquals("Message", x);
        }

        assertEquals(1000, aReceivedResponses.size());
        for (String x : aReceivedResponses)
        {
            assertEquals("Response", x);
        }
    }
    
    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected IDuplexOutputChannel myDuplexOutputChannel;
    protected IDuplexInputChannel myDuplexInputChannel;

    protected IDuplexStringMessageSender myMessageRequester;
    protected IDuplexStringMessageReceiver myMessageResponser;
}
