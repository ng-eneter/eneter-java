package eneter.messaging.endpoints.stringmessages;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

public abstract class StringMessagesBaseTester
{
    protected void setup(IMessagingSystemFactory messagingSystemFactory, String channelId) throws Exception
    {
        myMessagingSystemFactory = messagingSystemFactory;

        myOutputChannel = myMessagingSystemFactory.createOutputChannel(channelId);
        myInputChannel = myMessagingSystemFactory.createInputChannel(channelId);

        IStringMessagesFactory aMessageFactory = new StringMessagesFactory();
        myMessageSender = aMessageFactory.CreateStringMessageSender();
        myMessageReceiver = aMessageFactory.CreateStringMessageReceiver();
    }

    @Test
    public void sendReceive_1Message()
        throws Exception
    {
        myMessageSender.attachOutputChannel(myOutputChannel);

        // The test can be performed from more thread therefore we must synchronize.
        final ManualResetEvent aMessageReceivedEvent = new ManualResetEvent(false);

        final String[] aReceivedMessage = {""};
        myMessageReceiver.messageReceived().subscribe(new EventHandler<StringMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, StringMessageEventArgs y) throws Exception
            {
                aReceivedMessage[0] = y.getMessage();

                // Signal that the message was received.
                aMessageReceivedEvent.set();
            }
        });
        myMessageReceiver.attachInputChannel(myInputChannel);

        try
        {
            myMessageSender.sendMessage("Message");

            // Wait for the signal that the message is received.
            assertTrue(aMessageReceivedEvent.waitOne(200));
        }
        finally
        {
            myMessageReceiver.detachInputChannel();
        }

        // Check received values
        assertEquals("Message", aReceivedMessage[0]);
    }

    @Test
    public void sendReceive_MultiThreadAccess_1000Messages()
        throws Exception
    {
        myMessageSender.attachOutputChannel(myOutputChannel);

        // The test can be performed from more thread therefore we must synchronize.
        final ManualResetEvent aMessageReceivedEvent = new ManualResetEvent(false);

        final ArrayList<String> aReceivedMessages = new ArrayList<String>();
        myMessageReceiver.messageReceived().subscribe(new EventHandler<StringMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, StringMessageEventArgs y) throws Exception
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add(y.getMessage());
                }

                if (aReceivedMessages.size() == 1000)
                {
                    // Signal that the message was received.
                    aMessageReceivedEvent.set();
                }
            }
        });
        
        myMessageReceiver.attachInputChannel(myInputChannel);

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
                                    myMessageSender.sendMessage("Message");
                                    Thread.sleep(1);
                                } catch (Exception e)
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
            assertTrue(aMessageReceivedEvent.waitOne(10000));
        }
        finally
        {
            myMessageReceiver.detachInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessages.size());
        
        for (String m : aReceivedMessages)
        {
            assertEquals("Message", m);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void attachOutputChannelAgain() throws Exception
    {
        try
        {
            myMessageSender.attachOutputChannel(myOutputChannel);

            // The second attach should throw the exception.
            IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(myOutputChannel.getChannelId());
            myMessageSender.attachOutputChannel(anOutputChannel);
        }
        finally
        {
            myMessageSender.detachOutputChannel();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void attachInputChannelAgain()
        throws Exception
    {
        try
        {
            myMessageReceiver.attachInputChannel(myInputChannel);

            // The second attach should throw the exception.
            IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(myInputChannel.getChannelId());
            myMessageReceiver.attachInputChannel(anInputChannel);
        }
        finally
        {
            myMessageReceiver.detachInputChannel();
        }
    }

    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected IOutputChannel myOutputChannel;
    protected IInputChannel myInputChannel;

    protected IStringMessageSender myMessageSender;
    protected IStringMessageReceiver myMessageReceiver;
}
