package eneter.messaging.endpoints.typedmessages;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;

public abstract class TypedReliableMessagesBaseTester
{
    protected void Setup(IMessagingSystemFactory messagingSystemFactory, String channelId, ISerializer serializer) throws Exception
    {
        MessagingSystemFactory = messagingSystemFactory;

        DuplexOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(channelId);
        DuplexInputChannel = MessagingSystemFactory.createDuplexInputChannel(channelId);

        IReliableTypedMessagesFactory aMessageFactory = new ReliableTypedMessagesFactory(12000, serializer);
        MessageSender = aMessageFactory.createReliableDuplexTypedMessageSender(Integer.class, Integer.class);
        MessageReceiver = aMessageFactory.createReliableDuplexTypedMessageReceiver(Integer.class, Integer.class);

        mySerializer = serializer;
    }
    
    @Test
    public void sendReceive_1Message() throws Exception
    {
        final AutoResetEvent aMessagesProcessedEvent = new AutoResetEvent(false);

        final String[] aSentResponseMessageId = { "aaa" };
        final int[] aReceivedMessage = { 0 };
        MessageReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                aReceivedMessage[0] = y.getRequestMessage();

                try
                {
                    // Send the response
                    aSentResponseMessageId[0] = MessageReceiver.sendResponseMessage(y.getResponseReceiverId(), 1000);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response message failed.", err);
                }
            }
        });

        final String[] aDeliveredResponseMessage = { "bbb" };
        MessageReceiver.responseMessageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                aDeliveredResponseMessage[0] = y.getMessageId();
                aMessagesProcessedEvent.set();
            }
        });
        

        final int[] aReceivedResponse = { 0 };
        MessageSender.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<Integer> y)
            {
                aReceivedResponse[0] = y.getResponseMessage();
            }
        });

        final String[] aDeliveredMessageId = { "ccc" };
        MessageSender.messageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                aDeliveredMessageId[0] = y.getMessageId();
            }
        });
        

        String aSentMessageId = "ddd";
        try
        {
            MessageReceiver.attachDuplexInputChannel(DuplexInputChannel);
            Thread.sleep(100);
            MessageSender.attachDuplexOutputChannel(DuplexOutputChannel);

            aSentMessageId = MessageSender.sendRequestMessage(2000);

            // Wait for the signal that the message is received.
            assertTrue(aMessagesProcessedEvent.waitOne(200));
        }
        finally
        {
            MessageSender.detachDuplexOutputChannel();
            MessageReceiver.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(2000, aReceivedMessage[0]);
        assertEquals(1000, aReceivedResponse[0]);

        assertEquals(aSentMessageId, aDeliveredMessageId[0]);
        assertEquals(aSentResponseMessageId[0], aDeliveredResponseMessage[0]);
    }
    
    @Test
    public void sendReceive_MultiThreadAccess_1000Messages() throws Exception
    {
        final AutoResetEvent aMessagesProcessedEvent = new AutoResetEvent(false);

        final ArrayList<String> aSentResponseMessageIds = new ArrayList<String>();
        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        MessageReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add(y.getRequestMessage());

                    try
                    {
                        // Send the response
                        String aSentResponseMessageId = MessageReceiver.sendResponseMessage(y.getResponseReceiverId(), y.getRequestMessage() + 1000);
                        aSentResponseMessageIds.add(aSentResponseMessageId);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Sending of response message failed.", err);
                    }
                }
            }
        });
        
        final ArrayList<String> aDeliveredResponseMessageIds = new ArrayList<String>();
        MessageReceiver.responseMessageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                synchronized (aDeliveredResponseMessageIds)
                {
                    String aDeliveredResponseMessageId = y.getMessageId();
                    aDeliveredResponseMessageIds.add(aDeliveredResponseMessageId);

                    if (aDeliveredResponseMessageIds.size() == 1000)
                    {
                        aMessagesProcessedEvent.set();
                    }
                }
            }
        });
        

        final ArrayList<Integer> aReceivedResponses = new ArrayList<Integer>();
        MessageSender.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<Integer> y)
            {
                synchronized (aReceivedResponses)
                {
                    aReceivedResponses.add(y.getResponseMessage());
                }
            }
        });

        final ArrayList<String> aDeliveredMessageIds = new ArrayList<String>();
        MessageSender.messageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                synchronized (aDeliveredMessageIds)
                {
                    aDeliveredMessageIds.add(y.getMessageId());
                }
            }
        });

        ArrayList<String> aSentMessageIds = new ArrayList<String>();
        try
        {
            MessageReceiver.attachDuplexInputChannel(DuplexInputChannel);
            Thread.sleep(100);
            MessageSender.attachDuplexOutputChannel(DuplexOutputChannel);

            for (int i = 0; i < 1000; ++i)
            {
                String aSentMessageId = MessageSender.sendRequestMessage(i);
                aSentMessageIds.add(aSentMessageId);
            }

            // Wait for the signal that the message is received.
            assertTrue(aMessagesProcessedEvent.waitOne(50000));
        }
        finally
        {
            MessageSender.detachDuplexOutputChannel();
            MessageReceiver.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessages.size());
        Collections.sort(aReceivedMessages);
        for (int i = 0; i < 1000; ++i)
        {
            assertEquals(i, aReceivedMessages.get(i).intValue());
        }

        assertEquals(1000, aReceivedResponses.size());
        Collections.sort(aReceivedResponses);
        for (int i = 0; i < 1000; ++i)
        {
            assertEquals(i + 1000, aReceivedResponses.get(i).intValue());
        }

        assertEquals(1000, aDeliveredMessageIds.size());
        assertEquals(aDeliveredMessageIds.size(), aSentMessageIds.size());
        Collections.sort(aDeliveredMessageIds);
        Collections.sort(aSentMessageIds);
        for (int i = 0; i < aDeliveredMessageIds.size(); ++i)
        {
            assertTrue(aDeliveredMessageIds.get(i).equals(aSentMessageIds.get(i)));
        }

        assertEquals(1000, aDeliveredResponseMessageIds.size());
        Collections.sort(aDeliveredResponseMessageIds);
        Collections.sort(aSentResponseMessageIds);
        for (int i = 0; i < aDeliveredResponseMessageIds.size(); ++i)
        {
            assertTrue(aDeliveredResponseMessageIds.get(i).equals(aSentResponseMessageIds.get(i)));
        }
    }
    
    @Test
    public void notAcknowledgedRequest() throws Exception
    {
        // To simulate not delivering the acknowledge for the request, the receiver is not reliable.
        IDuplexTypedMessageReceiver<Integer, Integer> aReceiver = new DuplexTypedMessagesFactory(mySerializer).createDuplexTypedMessageReceiver(Integer.class, Integer.class);
        IReliableTypedMessageSender<Integer, Integer> aSender = new ReliableTypedMessagesFactory(500, mySerializer).createReliableDuplexTypedMessageSender(Integer.class, Integer.class);

        final AutoResetEvent aMessagesProcessedEvent = new AutoResetEvent(false);

        final String[] anAcknowledId = {""};
        aSender.messageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                anAcknowledId[0] = y.getMessageId();

                aMessagesProcessedEvent.set();
            }
        });
        
        final String[] aNotAcknowledgedId = {""};
        aSender.messageNotDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                aNotAcknowledgedId[0] = y.getMessageId();
                
                aMessagesProcessedEvent.set();
            }
        });

        String aSentMessageId;
        try
        {
            aReceiver.attachDuplexInputChannel(DuplexInputChannel);
            Thread.sleep(100);
            aSender.attachDuplexOutputChannel(DuplexOutputChannel);

            aSentMessageId = aSender.sendRequestMessage(2000);

            // Wait for the signal that the message is received.
            assertTrue(aMessagesProcessedEvent.waitOne(2000));
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals("", anAcknowledId[0]);
        assertEquals(aSentMessageId, aNotAcknowledgedId[0]);
    }
    
    @Test
    public void notAcknowledgedResponse() throws Exception
    {
        // To simulate not delivering the acknowledge for the request, the receiver is not reliable.
        IReliableTypedMessageReceiver<Integer, Integer> aReceiver = new ReliableTypedMessagesFactory(500, mySerializer).createReliableDuplexTypedMessageReceiver(Integer.class, Integer.class);
        IDuplexTypedMessageSender<Integer, Integer> aSender = new DuplexTypedMessagesFactory(mySerializer).createDuplexTypedMessageSender(Integer.class, Integer.class);

        final AutoResetEvent aMessagesProcessedEvent = new AutoResetEvent(false);

        final String[] anAcknowledId = {""};
        aReceiver.responseMessageDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                anAcknowledId[0] = y.getMessageId();

                aMessagesProcessedEvent.set();
            }
        });
        
        final String[] aNotAcknowledgedId = {""};
        aReceiver.responseMessageNotDelivered().subscribe(new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object x, ReliableMessageIdEventArgs y)
            {
                aNotAcknowledgedId[0] = y.getMessageId();
                
                aMessagesProcessedEvent.set();
            }
        });

        String aSentMessageId;
        try
        {
            aReceiver.attachDuplexInputChannel(DuplexInputChannel);
            Thread.sleep(100);
            aSender.attachDuplexOutputChannel(DuplexOutputChannel);

            aSentMessageId = aReceiver.sendResponseMessage(aSender.getAttachedDuplexOutputChannel().getResponseReceiverId(), 2000);

            // Wait for the signal that the message is received.
            assertTrue(aMessagesProcessedEvent.waitOne(2000));
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals("", anAcknowledId[0]);
        assertEquals(aSentMessageId, aNotAcknowledgedId[0]);
    }
    
    
    protected IMessagingSystemFactory MessagingSystemFactory;
    protected IDuplexOutputChannel DuplexOutputChannel;
    protected IDuplexInputChannel DuplexInputChannel;

    protected ISerializer mySerializer;
    protected IReliableTypedMessageSender<Integer, Integer> MessageSender;
    protected IReliableTypedMessageReceiver<Integer, Integer> MessageReceiver;
}
