package eneter.messaging.nodes.broker;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.net.system.IMethod2;

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
        aBroker.attachDuplexInputChannel(aBrokerInputChannel);
        
        IDuplexBrokerClient aBrokerClient1 = aBrokerFactory.createBrokerClient();
        
        final BrokerMessageReceivedEventArgs[] aClient1ReceivedMessage = {null};
        aBrokerClient1.brokerMessageReceived().subscribe(new IMethod2<Object, BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, BrokerMessageReceivedEventArgs y)
                    throws Exception
            {
                aClient1ReceivedMessage[0] = y;
            }
        });
        aBrokerClient1.attachDuplexOutputChannel(aSubscriber1ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient2 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient2ReceivedMessage = {null};
        aBrokerClient2.brokerMessageReceived().subscribe(new IMethod2<Object, BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, BrokerMessageReceivedEventArgs y)
                    throws Exception
            {
                aClient2ReceivedMessage[0] = y;
            }
        });
        aBrokerClient2.attachDuplexOutputChannel(aSubscriber2ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient3 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient3ReceivedMessage = {null};
        aBrokerClient3.brokerMessageReceived().subscribe(new IMethod2<Object, BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, BrokerMessageReceivedEventArgs y)
                    throws Exception
            {
                aClient3ReceivedMessage[0] = y;
            }
        });
        aBrokerClient3.attachDuplexOutputChannel(aSubscriber3ClientOutputChannel);

        IDuplexBrokerClient aBrokerClient4 = aBrokerFactory.createBrokerClient();
        final BrokerMessageReceivedEventArgs[] aClient4ReceivedMessage = {null};
        aBrokerClient4.brokerMessageReceived().subscribe(new IMethod2<Object, BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, BrokerMessageReceivedEventArgs y)
                    throws Exception
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

        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;

        aBrokerClient2.unsubscribe();
        
        aBrokerClient3.sendMessage("MTypeC", "Message MTC");

        assertEquals(null, aClient1ReceivedMessage[0]);

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals("MTypeC", aClient3ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message MTC", (String)aClient3ReceivedMessage[0].getMessage());
        assertEquals(null, aClient3ReceivedMessage[0].getReceivingError());

        assertEquals(null, aClient4ReceivedMessage[0]);

        
        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;

        aBrokerClient3.sendMessage("TypeA", "Message A");
        assertEquals("TypeA", aClient1ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient1ReceivedMessage[0].getMessage());
        assertEquals(null, aClient1ReceivedMessage[0].getReceivingError());

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals(null, aClient3ReceivedMessage[0]);

        assertEquals("TypeA", aClient4ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient4ReceivedMessage[0].getMessage());
        assertEquals(null, aClient4ReceivedMessage[0].getReceivingError());


        aClient1ReceivedMessage[0] = null;
        aClient2ReceivedMessage[0] = null;
        aClient3ReceivedMessage[0] = null;
        aClient4ReceivedMessage[0] = null;


        String[] aNewMessageType = { "TypeA" };
        aBrokerClient3.subscribe(aNewMessageType);
        
        aBrokerClient1.detachDuplexOutputChannel();

        aBrokerClient3.sendMessage("TypeA", "Message A");
        assertEquals(null, aClient1ReceivedMessage[0]);

        assertEquals(null, aClient2ReceivedMessage[0]);

        assertEquals("TypeA", aClient3ReceivedMessage[0].getMessageTypeId());
        assertEquals("Message A", (String)aClient3ReceivedMessage[0].getMessage());
        assertEquals(null, aClient3ReceivedMessage[0].getReceivingError());
    }
}
