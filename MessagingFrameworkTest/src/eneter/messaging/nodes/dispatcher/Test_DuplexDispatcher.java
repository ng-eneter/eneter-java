package eneter.messaging.nodes.dispatcher;

import static org.junit.Assert.*;

import org.junit.*;

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
        myStringMessageReceiver1.requestReceived().subscribe(new IMethod2<Object, StringRequestReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringRequestReceivedEventArgs y)
                    throws Exception
            {
                aReceivedMessage1[0] = y.getRequestMessage();
                myStringMessageReceiver1.sendResponseMessage(y.getResponseReceiverId(), "Response1");
            }
        });

        final String[] aReceivedMessage2 = {""};
        myStringMessageReceiver2.requestReceived().subscribe(new IMethod2<Object, StringRequestReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringRequestReceivedEventArgs y)
                    throws Exception
            {
                aReceivedMessage2[0] = y.getRequestMessage();
                myStringMessageReceiver2.sendResponseMessage(y.getResponseReceiverId(), "Response2");
            }
        });
        
        final String[] aReceivedMessage3 = {""};
        myStringMessageReceiver3.requestReceived().subscribe(new IMethod2<Object, StringRequestReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringRequestReceivedEventArgs y)
                    throws Exception
            {
                aReceivedMessage3[0] = y.getRequestMessage();
                myStringMessageReceiver3.sendResponseMessage(y.getResponseReceiverId(), "Response3");
            }
        });
        

        final String[] aReceivedResponse11 = {""};
        myStringMessageSender11.responseReceived().subscribe(new IMethod2<Object, StringResponseReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringResponseReceivedEventArgs y)
                    throws Exception
            {
                aReceivedResponse11[0] += y.getResponseMessage();
            }
        });

        final String[] aReceivedResponse12 = {""};
        myStringMessageSender12.responseReceived().subscribe(new IMethod2<Object, StringResponseReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringResponseReceivedEventArgs y)
                    throws Exception
            {
                aReceivedResponse12[0] += y.getResponseMessage();
            }
        });

        final String[] aReceivedResponse13 = {""};
        myStringMessageSender13.responseReceived().subscribe(new IMethod2<Object, StringResponseReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringResponseReceivedEventArgs y)
                    throws Exception
            {
                aReceivedResponse13[0] += y.getResponseMessage();
            }
        });
        
        final String[] aReceivedResponse22 = {""};
        myStringMessageSender22.responseReceived().subscribe(new IMethod2<Object, StringResponseReceivedEventArgs>()
        {
            @Override
            public void invoke(Object x, StringResponseReceivedEventArgs y)
                    throws Exception
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



        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";
        aReceivedMessage3[0] = "";
        aReceivedResponse11[0] = "";
        aReceivedResponse12[0] = "";
        aReceivedResponse13[0] = "";
        aReceivedResponse22[0] = "";
        myDuplexDispatcher.detachDuplexInputChannel("ChannelA_2");
        myDuplexDispatcher.attachDuplexInputChannel(anInputChannelA_2);
        
        myDuplexDispatcher.addDuplexOutputChannel("ChannelB_2");

        myStringMessageSender12.sendMessage("Message2");

        assertEquals("", aReceivedMessage1[0]);
        assertEquals("Message2", aReceivedMessage2[0]);
        assertEquals("", aReceivedMessage3[0]);

        assertEquals("", aReceivedResponse11[0]);
        assertEquals("Response2", aReceivedResponse12[0]);
        assertEquals("", aReceivedResponse13[0]);
        assertEquals("", aReceivedResponse22[0]);
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
