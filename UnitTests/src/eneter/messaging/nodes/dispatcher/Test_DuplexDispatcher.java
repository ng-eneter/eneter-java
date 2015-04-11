package eneter.messaging.nodes.dispatcher;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.stringmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.*;
import eneter.net.system.*;

public class Test_DuplexDispatcher
{
    @Before
    public void setup()
    {
        IDuplexStringMessagesFactory aDuplexStringMessagesFactory = new DuplexStringMessagesFactory();
        myStringMessageSender11 = aDuplexStringMessagesFactory.createDuplexStringMessageSender();
        myStringMessageSender12 = aDuplexStringMessagesFactory.createDuplexStringMessageSender();
        myStringMessageSender13 = aDuplexStringMessagesFactory.createDuplexStringMessageSender();
        myStringMessageSender22 = aDuplexStringMessagesFactory.createDuplexStringMessageSender();
        myStringMessageReceiver1 = aDuplexStringMessagesFactory.createDuplexStringMessageReceiver();
        myStringMessageReceiver2 = aDuplexStringMessagesFactory.createDuplexStringMessageReceiver();
        myStringMessageReceiver3 = aDuplexStringMessagesFactory.createDuplexStringMessageReceiver();


        myMessagingSystemFactory = new SynchronousMessagingSystemFactory();
        IDuplexDispatcherFactory aDuplexDispatcherFactory = new DuplexDispatcherFactory(myMessagingSystemFactory);
        myDuplexDispatcher = aDuplexDispatcherFactory.createDuplexDispatcher();
    }

    @Test
    public void sendAndReceive() throws Exception
    {
        IDuplexInputChannel anInputChannelA_1 = myMessagingSystemFactory.createDuplexInputChannel("ChannelA_1");
        IDuplexInputChannel anInputChannelA_2 = myMessagingSystemFactory.createDuplexInputChannel("ChannelA_2");
        IDuplexInputChannel anInputChannelA_3 = myMessagingSystemFactory.createDuplexInputChannel("ChannelA_3");
        
        myDuplexDispatcher.attachDuplexInputChannel(anInputChannelA_1);
        myDuplexDispatcher.attachDuplexInputChannel(anInputChannelA_2);
        myDuplexDispatcher.attachDuplexInputChannel(anInputChannelA_3);

        IDuplexOutputChannel aChanelA_1 = myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_1");
        myStringMessageSender11.attachDuplexOutputChannel(aChanelA_1);
        IDuplexOutputChannel aChanelA_2 = myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_2");
        myStringMessageSender12.attachDuplexOutputChannel(aChanelA_2);
        IDuplexOutputChannel aChanelA_3 = myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_3");
        myStringMessageSender13.attachDuplexOutputChannel(aChanelA_3);

        // Note: another client sending to address 'ChannelA_2'.
        IDuplexOutputChannel aChanelA_2_2 = myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_2");
        myStringMessageSender22.attachDuplexOutputChannel(aChanelA_2_2);

        
        IDuplexInputChannel aCannelB_1 = myMessagingSystemFactory.createDuplexInputChannel("ChannelB_1");
        myStringMessageReceiver1.attachDuplexInputChannel(aCannelB_1);
        IDuplexInputChannel aCannelB_2 = myMessagingSystemFactory.createDuplexInputChannel("ChannelB_2");
        myStringMessageReceiver2.attachDuplexInputChannel(aCannelB_2);
        IDuplexInputChannel aCannelB_3 = myMessagingSystemFactory.createDuplexInputChannel("ChannelB_3");
        myStringMessageReceiver3.attachDuplexInputChannel(aCannelB_3);

        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_1");
        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_2");
        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_3");

        final String[] aReceivedMessage1 = {""};
        myStringMessageReceiver1.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aReceivedMessage1[0] = y.getRequestMessage();
                try
                {
					myStringMessageReceiver1.sendResponseMessage(y.getResponseReceiverId(), "Response1");
				}
                catch (Exception err)
                {
					EneterTrace.error("Sending Response1 failed.", err);
				}
            }
        });

        final String[] aReceivedMessage2 = {""};
        myStringMessageReceiver2.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aReceivedMessage2[0] = y.getRequestMessage();
                try
                {
					myStringMessageReceiver2.sendResponseMessage(y.getResponseReceiverId(), "Response2");
				}
                catch (Exception err)
                {
                	EneterTrace.error("Sending Response2 failed.", err);
				}
            }
        });
        
        final String[] aReceivedMessage3 = {""};
        myStringMessageReceiver3.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aReceivedMessage3[0] = y.getRequestMessage();
                try
                {
					myStringMessageReceiver3.sendResponseMessage(y.getResponseReceiverId(), "Response3");
				}
                catch (Exception err)
                {
                	EneterTrace.error("Sending Response3 failed.", err);
				}
            }
        });
        

        final String[] aReceivedResponse11 = {""};
        myStringMessageSender11.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse11[0] += y.getResponseMessage();
            }
        });

        final String[] aReceivedResponse12 = {""};
        myStringMessageSender12.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse12[0] += y.getResponseMessage();
            }
        });

        final String[] aReceivedResponse13 = {""};
        myStringMessageSender13.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse13[0] += y.getResponseMessage();
            }
        });
        
        final String[] aReceivedResponse22 = {""};
        myStringMessageSender22.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse22[0] += y.getResponseMessage();
            }
        });


        myStringMessageSender11.sendMessage("Message1");
        
        assertEquals("Message1", aReceivedMessage1[0]);
        assertEquals("Message1", aReceivedMessage2[0]);
        assertEquals("Message1", aReceivedMessage3[0]);

        assertEquals("Response1Response2Response3", aReceivedResponse11[0]);
        assertEquals("", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);


        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myStringMessageSender12.sendMessage("Message2");

        assertEquals("Message2", aReceivedMessage1[0]);
        assertEquals("Message2", aReceivedMessage2[0]);
        assertEquals("Message2", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("Response1Response2Response3", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);


        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myStringMessageSender22.sendMessage("Message22");

        assertEquals("Message22", aReceivedMessage1[0]);
        assertEquals("Message22", aReceivedMessage2[0]);
        assertEquals("Message22", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("Response1Response2Response3", aReceivedResponse22[0]);


        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myDuplexDispatcher.removeDuplexOutputChannel("ChannelB_2");
        myStringMessageSender12.sendMessage("Message2");

        assertEquals("Message2", aReceivedMessage1[0]);
        assertEquals("", aReceivedMessage2[0]);
        assertEquals("Message2", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("Response1Response3", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);


        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myDuplexDispatcher.removeDuplexOutputChannel("ChannelB_2");
        myStringMessageSender12.sendMessage("Message2");

        assertEquals("Message2", aReceivedMessage1[0]);
        assertEquals("", aReceivedMessage2[0]);
        assertEquals("Message2", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("Response1Response3", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);


        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myDuplexDispatcher.removeAllDuplexOutputChannels();
        myStringMessageSender12.sendMessage("Message2");

        assertEquals("", aReceivedMessage1[0]);
        assertEquals("", aReceivedMessage2[0]);
        assertEquals("", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);



    }
    
    //@Test
    public void GetAssociatedResponseReceiverId() throws Exception
    {
        myDuplexDispatcher.attachDuplexInputChannel(myMessagingSystemFactory.createDuplexInputChannel("ChannelA_1"));
        myDuplexDispatcher.attachDuplexInputChannel(myMessagingSystemFactory.createDuplexInputChannel("ChannelA_2"));
        
        myStringMessageSender11.attachDuplexOutputChannel(myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_1"));
        myStringMessageSender12.attachDuplexOutputChannel(myMessagingSystemFactory.createDuplexOutputChannel("ChannelA_2"));

        myStringMessageReceiver1.attachDuplexInputChannel(myMessagingSystemFactory.createDuplexInputChannel("ChannelB_1"));
        myStringMessageReceiver2.attachDuplexInputChannel(myMessagingSystemFactory.createDuplexInputChannel("ChannelB_2"));

        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_1");
        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_2");

        final String[] aResponseReceiverId1 = { "" };
        myStringMessageReceiver1.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aResponseReceiverId1[0] = y.getResponseReceiverId();
            }
        });

        final String[] aResponseReceiverId2 = { "" };
        myStringMessageReceiver2.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aResponseReceiverId2[0] = y.getResponseReceiverId();
            }
        });

        myStringMessageSender11.sendMessage("Message1");
        String aClientId1FromReceiver1 = myDuplexDispatcher.getAssociatedResponseReceiverId(aResponseReceiverId1[0]);
        String aClientId1FromReceiver2 = myDuplexDispatcher.getAssociatedResponseReceiverId(aResponseReceiverId2[0]);
        assertEquals(myStringMessageSender11.getAttachedDuplexOutputChannel().getResponseReceiverId(), aClientId1FromReceiver1);
        assertEquals(myStringMessageSender11.getAttachedDuplexOutputChannel().getResponseReceiverId(), aClientId1FromReceiver2);

        myStringMessageSender12.sendMessage("Message2");
        String aClientId2FromReceiver1 = myDuplexDispatcher.getAssociatedResponseReceiverId(aResponseReceiverId1[0]);
        String aClientId2FromReceiver2 = myDuplexDispatcher.getAssociatedResponseReceiverId(aResponseReceiverId2[0]);
        assertEquals(myStringMessageSender12.getAttachedDuplexOutputChannel().getResponseReceiverId(), aClientId2FromReceiver1);
        assertEquals(myStringMessageSender12.getAttachedDuplexOutputChannel().getResponseReceiverId(), aClientId2FromReceiver2);
    }
    
    
    private IMessagingSystemFactory myMessagingSystemFactory;
    private IDuplexDispatcher myDuplexDispatcher;
    
    private IDuplexStringMessageSender myStringMessageSender11;
    private IDuplexStringMessageSender myStringMessageSender12;
    private IDuplexStringMessageSender myStringMessageSender13;
    
    private IDuplexStringMessageSender myStringMessageSender22;

    private IDuplexStringMessageReceiver myStringMessageReceiver1;
    private IDuplexStringMessageReceiver myStringMessageReceiver2;
    private IDuplexStringMessageReceiver myStringMessageReceiver3;
}
