package eneter.messaging.nodes.broker;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.JavaBinarySerializer;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;

public class Test_Broker
{
    @Test
    public void notifySubscribers() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        
        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriber1ClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriber2ClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriber3ClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriber4ClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");

        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();

        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        final BrokerMessageReceivedEventArgs[] aBrokerReceivedMessage = { null };
        aBroker.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object sender, BrokerMessageReceivedEventArgs y)
            {
                aBrokerReceivedMessage[0] = y;
            }
        });
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);
        
        IDuplexBrokerClient aBrokerClient1 = aBrokerFactory.createBrokerClient();
        
        final BrokerMessageReceivedEventArgs[] aClient1ReceivedMessage = {null};
        aBrokerClient1.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient1ReceivedMessage[0] = y;
            }
        });
        aBrokerClient1.attachDuplexOutputChannel(aSubscriber1ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient2 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient2ReceivedMessage = {null};
        aBrokerClient2.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient2ReceivedMessage[0] = y;
            }
        });
        aBrokerClient2.attachDuplexOutputChannel(aSubscriber2ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient3 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient3ReceivedMessage = {null};
        aBrokerClient3.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient3ReceivedMessage[0] = y;
            }
        });
        aBrokerClient3.attachDuplexOutputChannel(aSubscriber3ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient4 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient4ReceivedMessage = {null};
        aBrokerClient4.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient4ReceivedMessage[0] = y;
            }
        });
        aBrokerClient4.attachDuplexOutputChannel(aSubscriber4ClientOutputChannel);

        String[] aSubscription1 = {"TypeA", "TypeB"};
        aBrokerClient1.subscribe(aSubscription1);

        String[] aSubscription2 = { "TypeA" };
        aBrokerClient2.subscribe(aSubscription2);

        String[] aSubscription3 = { "MTypeC" };
        aBrokerClient3.subscribe(aSubscription3);

        // Subscription using the regular expression.
        // Note: Subscribe for all message types starting with the character 'T'. 
        String[] aSubscription4 = { "^T.*" };
        aBrokerClient4.subscribeRegExp(aSubscription4);
        
        aBroker.subscribe("TypeA");


        aBrokerClient3.sendMessage("TypeA", "Message A");
        assertEquals("TypeA", aClient1ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient1ReceivedMessage[0].getMessage());
        assertEquals(null, aClient1ReceivedMessage[0].getReceivingError());
        
        assertEquals("TypeA", aClient2ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient2ReceivedMessage[0].getMessage());
        assertEquals(null, aClient2ReceivedMessage[0].getReceivingError());

        assertEquals(null, aClient3ReceivedMessage[0]);

        assertEquals("TypeA", aClient4ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient4ReceivedMessage[0].getMessage());
        assertEquals(null, aClient4ReceivedMessage[0].getReceivingError());
        
        assertEquals("TypeA", aBrokerReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aBrokerReceivedMessage[0].getMessage());
        assertEquals(null, aBrokerReceivedMessage[0].getReceivingError());

        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;
        aBrokerReceivedMessage[0] = null;

        aBrokerClient2.unsubscribe();
        
        aBrokerClient3.sendMessage("MTypeC", "Message MTC");

        assertEquals(null, aClient1ReceivedMessage[0]);

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals("MTypeC", aClient3ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message MTC", (String)aClient3ReceivedMessage[0].getMessage());
        assertEquals(null, aClient3ReceivedMessage[0].getReceivingError());

        assertEquals(null, aClient4ReceivedMessage[0]);
        
        assertEquals(null, aBrokerReceivedMessage[0]);

        
        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;
        aBrokerReceivedMessage[0] = null;

        aBrokerClient3.sendMessage("TypeA", "Message A");
        assertEquals("TypeA", aClient1ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient1ReceivedMessage[0].getMessage());
        assertEquals(null, aClient1ReceivedMessage[0].getReceivingError());

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals(null, aClient3ReceivedMessage[0]);

        assertEquals("TypeA", aClient4ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient4ReceivedMessage[0].getMessage());
        assertEquals(null, aClient4ReceivedMessage[0].getReceivingError());
        
        assertEquals("TypeA", aBrokerReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aBrokerReceivedMessage[0].getMessage());
        assertEquals(null, aBrokerReceivedMessage[0].getReceivingError());


        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;
        aBrokerReceivedMessage[0] = null;


        aBroker.unsubscribe("TypeA");
        
        String[] aNewMessageType = { "TypeA" };
        aBrokerClient3.subscribe(aNewMessageType);
        
        aBrokerClient1.detachDuplexOutputChannel();

        aBrokerClient3.sendMessage("TypeA", "Message A");
        assertEquals(null, aClient1ReceivedMessage[0]);

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals("TypeA", aClient3ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient3ReceivedMessage[0].getMessage());
        assertEquals(null, aClient3ReceivedMessage[0].getReceivingError());
        
        assertEquals(null, aBrokerReceivedMessage[0]);
    }
    
    @Test
    public void subscribeSameMessageTwice() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        
        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriberClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aPublisherClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");

        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();

        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);
        
        IDuplexBrokerClient aSubscriber = aBrokerFactory.createBrokerClient();
        final ArrayList<BrokerMessageReceivedEventArgs> aClient1ReceivedMessage = new ArrayList<BrokerMessageReceivedEventArgs>();
        aSubscriber.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient1ReceivedMessage.add(y);
            }
        });
        aSubscriber.attachDuplexOutputChannel(aSubscriberClientOutputChannel);

        IDuplexBrokerClient aPublisher = aBrokerFactory.createBrokerClient();
        aPublisher.attachDuplexOutputChannel(aPublisherClientOutputChannel);

        // Subscribe the 1st time.
        aSubscriber.subscribe("TypeA");
        
        // Subscribe the 2nd time.
        aSubscriber.subscribe("TypeA");
        
        // Notify the message.
        aPublisher.sendMessage("TypeA", "Message A");
        
        // Although the client is subscribed twice, the message shall be notified once.
        assertEquals(1, aClient1ReceivedMessage.size());
        assertEquals("TypeA", aClient1ReceivedMessage.get(0).getMessageTypeId());
        assertEquals("Message A", (String)aClient1ReceivedMessage.get(0).getMessage());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void subscribeNullMessageType() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        
        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("BrokerChannel");
        IDuplexOutputChannel aSubscriberClientOutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");

        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
        
        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);

        IDuplexBrokerClient aSubscriber = aBrokerFactory.createBrokerClient();
        final ArrayList<BrokerMessageReceivedEventArgs> aClient1ReceivedMessage = new ArrayList<BrokerMessageReceivedEventArgs>();
        aSubscriber.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
            {
                @Override
                public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
                {
                    aClient1ReceivedMessage.add(y);
                }
            });
        aSubscriber.attachDuplexOutputChannel(aSubscriberClientOutputChannel);

        // Subscribe.
        String[] aTypes = {"*", null};
        aSubscriber.subscribe(aTypes);
    }
    
    @Test
    public void DoNotNotifyPublisher() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();

        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("BrokerChannel");
        IDuplexOutputChannel aClient1OutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aClient2OutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");

        // Specify in the factory that the publisher shall not be notified from its own published events.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory(false);

        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);

        IDuplexBrokerClient aClient1 = aBrokerFactory.createBrokerClient();
        final ArrayList<BrokerMessageReceivedEventArgs> aClient1ReceivedMessage = new ArrayList<BrokerMessageReceivedEventArgs>();
        aClient1.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient1ReceivedMessage.add(y);
            }
        });
        aClient1.attachDuplexOutputChannel(aClient1OutputChannel);

        IDuplexBrokerClient aClient2 = aBrokerFactory.createBrokerClient();
        final ArrayList<BrokerMessageReceivedEventArgs> aClient2ReceivedMessage = new ArrayList<BrokerMessageReceivedEventArgs>();
        aClient2.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, BrokerMessageReceivedEventArgs y)
            {
                aClient2ReceivedMessage.add(y);
            }
        });
        aClient2.attachDuplexOutputChannel(aClient2OutputChannel);


        aClient1.subscribe("TypeA");
        aClient2.subscribe("TypeA");


        // Notify the message.
        aClient2.sendMessage("TypeA", "Message A");

        // Client 2 should not get the notification.
        assertEquals(1, aClient1ReceivedMessage.size());
        assertEquals(0, aClient2ReceivedMessage.size());
        assertEquals("TypeA", aClient1ReceivedMessage.get(0).getMessageTypeId());
        assertEquals("Message A", (String)aClient1ReceivedMessage.get(0).getMessage());
    }
    
    @Test
    public void Notify_50000() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();

        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("BrokerChannel");
        IDuplexOutputChannel aClient1OutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");
        IDuplexOutputChannel aClient2OutputChannel = aMessagingSystem.createDuplexOutputChannel("BrokerChannel");

        // Specify in the factory that the publisher shall not be notified from its own published events.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory(false, new JavaBinarySerializer());

        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);

        IDuplexBrokerClient aClient1 = aBrokerFactory.createBrokerClient();
        final int[] aCount = { 0 };
        aClient1.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
            {
                ++aCount[0];
            }
        });
        aClient1.attachDuplexOutputChannel(aClient1OutputChannel);

        IDuplexBrokerClient aClient2 = aBrokerFactory.createBrokerClient();
        aClient2.attachDuplexOutputChannel(aClient2OutputChannel);

        aClient1.subscribe("TypeA");

        for (int i = 0; i < 50000; ++i)
        {
            // Notify the message.
            aClient2.sendMessage("TypeA", "Message A");
        }

        // Client 2 should not get the notification.
        assertEquals(50000, aCount[0]);
    }
    
    @Test
    public void Notify_50000_TCP() throws Exception
    {
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        // Create channels
        IMessagingSystemFactory aMessagingSystem = new TcpMessagingSystemFactory();

        IDuplexInputChannel aBrokerInputChannel = aMessagingSystem.createDuplexInputChannel("tcp://127.0.0.1:" + aPort + "/");
        IDuplexOutputChannel aClient1OutputChannel = aMessagingSystem.createDuplexOutputChannel("tcp://127.0.0.1:" + aPort + "/");
        IDuplexOutputChannel aClient2OutputChannel = aMessagingSystem.createDuplexOutputChannel("tcp://127.0.0.1:" + aPort + "/");

        // Specify in the factory that the publisher shall not be notified from its own published events.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory(false, new JavaBinarySerializer());

        IDuplexBroker aBroker = aBrokerFactory.createBroker();
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);

        IDuplexBrokerClient aClient1 = aBrokerFactory.createBrokerClient();
        final int[] aCount = { 0 };
        final AutoResetEvent aCompletedEvent = new AutoResetEvent(false);
        aClient1.brokerMessageReceived().subscribe(new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            
            @Override
            public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
            {
                ++aCount[0];
                if (aCount[0] == 50000)
                {
                    aCompletedEvent.set();
                }
            }
        });
        aClient1.attachDuplexOutputChannel(aClient1OutputChannel);

        IDuplexBrokerClient aClient2 = aBrokerFactory.createBrokerClient();
        aClient2.attachDuplexOutputChannel(aClient2OutputChannel);

        try
        {
            aClient1.subscribe("TypeA");

            for (int i = 0; i < 50000; ++i)
            {
                // Notify the message.
                aClient2.sendMessage("TypeA", "Message A");
            }

            aCompletedEvent.waitOne();

            // Client 2 should not get the notification.
            assertEquals(50000, aCount[0]);
        }
        finally
        {
            aClient1.detachDuplexOutputChannel();
            aClient2.detachDuplexOutputChannel();
            aBroker.detachDuplexInputChannel();
        }
    }
}
