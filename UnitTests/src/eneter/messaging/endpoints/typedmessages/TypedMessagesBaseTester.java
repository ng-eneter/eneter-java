package eneter.messaging.endpoints.typedmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;

public abstract class TypedMessagesBaseTester
{
    protected void setup(IMessagingSystemFactory messagingSystemFactory, String channelId, ISerializer serializer) throws Exception
    {
        MessagingSystemFactory = messagingSystemFactory;

        OutputChannel = MessagingSystemFactory.createOutputChannel(channelId);
        InputChannel = MessagingSystemFactory.createInputChannel(channelId);

        ITypedMessagesFactory aMessageFactory = new TypedMessagesFactory(serializer);
        MessageSender = aMessageFactory.createTypedMessageSender(Fake_TypedMessage.class);
        MessageReceiver = aMessageFactory.createTypedMessageReceiver(Fake_TypedMessage.class);
    }

    @Test
    public void sendReceive_1Message() throws Exception
    {
        MessageSender.attachOutputChannel(OutputChannel);

        // The test can be performed from more thread therefore we must synchronize.
        final ManualResetEvent aMessageReceivedEvent = new ManualResetEvent(false);

        final Fake_TypedMessage[] aReceivedMessage = {null};
        MessageReceiver.messageReceived().subscribe(new EventHandler<TypedMessageReceivedEventArgs<Fake_TypedMessage>>()
        {
            @Override
            public void onEvent(Object x, TypedMessageReceivedEventArgs<Fake_TypedMessage> y)
            {
                aReceivedMessage[0] = y.getMessageData();

                // Signal that the message was received.
                aMessageReceivedEvent.set();
            }
        });
        MessageReceiver.attachInputChannel(InputChannel);

        try
        {
            Fake_TypedMessage aMessage = new Fake_TypedMessage();
            aMessage.FirstName = "Janko";
            aMessage.SecondName = "Mrkvicka";
            
            MessageSender.sendMessage(aMessage);

            // Wait for the signal that the message is received.
            assertTrue(aMessageReceivedEvent.waitOne(200));
        }
        finally
        {
            MessageReceiver.detachInputChannel();
        }

        // Check received values
        assertEquals("Janko", aReceivedMessage[0].FirstName);
        assertEquals("Mrkvicka", aReceivedMessage[0].SecondName);
    }

    @Test
    public void sendReceive_MultiThreadAccess_1000Messages() throws Exception
    {
        MessageSender.attachOutputChannel(OutputChannel);

        // The test can be performed from more thread therefore we must synchronize.
        final ManualResetEvent aMessageReceivedEvent = new ManualResetEvent(false);

        final ArrayList<Fake_TypedMessage> aReceivedMessages = new ArrayList<Fake_TypedMessage>();
        MessageReceiver.messageReceived().subscribe(new EventHandler<TypedMessageReceivedEventArgs<Fake_TypedMessage>>()
        {
            @Override
            public void onEvent(Object x, TypedMessageReceivedEventArgs<Fake_TypedMessage> y)
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add(y.getMessageData());
                }

                if (aReceivedMessages.size() == 1000)
                {
                    // Signal that the message was received.
                    aMessageReceivedEvent.set();
                }
            }
        });
        MessageReceiver.attachInputChannel(InputChannel);

        final Fake_TypedMessage aMessage = new Fake_TypedMessage();
        aMessage.FirstName = "Janko";
        aMessage.SecondName = "Mrkvicka";

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
                                MessageSender.sendMessage(aMessage);
                                Thread.sleep(1);
                            }
                            catch(Exception err)
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
            MessageReceiver.detachInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessages.size());
        
        for (Fake_TypedMessage x : aReceivedMessages)
        {
            assertEquals(aMessage.FirstName, x.FirstName);
        }
        
        for (Fake_TypedMessage x : aReceivedMessages)
        {
            assertEquals(aMessage.SecondName, x.SecondName);
        }
    }


    protected IMessagingSystemFactory MessagingSystemFactory;
    protected IOutputChannel OutputChannel;
    protected IInputChannel InputChannel;

    protected ITypedMessageSender<Fake_TypedMessage> MessageSender;
    protected ITypedMessageReceiver<Fake_TypedMessage> MessageReceiver;
}
