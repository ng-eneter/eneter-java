package eneter.messaging.nodes.channelwrapper;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.stringmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.*;
import eneter.net.system.EventHandler;

public class Tester_DuplexChannelWrapper
{
    @Before
    public void setup() throws Exception
    {
        myGlobalMessaging = new SynchronousMessagingSystemFactory();
        myDuplexGlobalOutputChannel = myGlobalMessaging.createDuplexOutputChannel("MainChannel");
        myDuplexGlobalInputChannel = myGlobalMessaging.createDuplexInputChannel("MainChannel");

        myLocalMessaging1 = new SynchronousMessagingSystemFactory();
        myLocalMessaging2 = new SynchronousMessagingSystemFactory();
        
        IChannelWrapperFactory aFactory = new ChannelWrapperFactory();
        myDuplexChannelWrapper = aFactory.createDuplexChannelWrapper();
        myDuplexChannelUnwrapper = aFactory.createDuplexChannelUnwrapper(myLocalMessaging2);
    }
    
    @Test
    public void WrapUnwrapMessage() throws Exception
    {
        // Wrapped/unwrapped channels
        String aChannel1Id = "Channel1Id";
        String aChannel2Id = "Channel2Id";

        IDuplexStringMessagesFactory aStringMessagesFactory = new DuplexStringMessagesFactory();
        
        final IDuplexStringMessageReceiver aStringMessageReceiver1 = aStringMessagesFactory.createDuplexStringMessageReceiver();
        final IDuplexStringMessageReceiver aStringMessageReceiver2 = aStringMessagesFactory.createDuplexStringMessageReceiver();

        final IDuplexStringMessageSender aStringMessageSender1 = aStringMessagesFactory.createDuplexStringMessageSender();
        final IDuplexStringMessageSender aStringMessageSender2 = aStringMessagesFactory.createDuplexStringMessageSender();

        
        // Sender side
        IDuplexInputChannel anInputChannel11 = myLocalMessaging1.createDuplexInputChannel(aChannel1Id);
        IDuplexOutputChannel anOutputChannel11 = myLocalMessaging1.createDuplexOutputChannel(aChannel1Id);
        
        IDuplexInputChannel anInputChannel12 = myLocalMessaging1.createDuplexInputChannel(aChannel2Id);
        IDuplexOutputChannel anOutputChannel12 = myLocalMessaging1.createDuplexOutputChannel(aChannel2Id);
        
        myDuplexChannelWrapper.attachDuplexInputChannel(anInputChannel11);
        aStringMessageSender1.attachDuplexOutputChannel(anOutputChannel11);
        
        myDuplexChannelWrapper.attachDuplexInputChannel(anInputChannel12);
        aStringMessageSender2.attachDuplexOutputChannel(anOutputChannel12);
        
        // Receiver side
        IDuplexInputChannel anInputChannel21 = myLocalMessaging2.createDuplexInputChannel(aChannel1Id);
        IDuplexInputChannel anInputChannel22 = myLocalMessaging2.createDuplexInputChannel(aChannel2Id);
        aStringMessageReceiver1.attachDuplexInputChannel(anInputChannel21);
        aStringMessageReceiver2.attachDuplexInputChannel(anInputChannel22);
        
        
        // Connect wrapper and unwrapper to global channels.
        myDuplexChannelUnwrapper.attachDuplexInputChannel(myDuplexGlobalInputChannel);
        myDuplexChannelWrapper.attachDuplexOutputChannel(myDuplexGlobalOutputChannel);


        final StringRequestReceivedEventArgs[] aReceivedMessage1 = {null};
        aStringMessageReceiver1.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aReceivedMessage1[0] = y;
                try
                {
					aStringMessageReceiver1.sendResponseMessage(y.getResponseReceiverId(), "Response1");
				}
                catch (Exception err)
                {
					EneterTrace.error("Sending Response1 failed.", err);
				}
            }
        });
        

        final StringRequestReceivedEventArgs[] aReceivedMessage2 = {null};
        aStringMessageReceiver2.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringRequestReceivedEventArgs y)
            {
                aReceivedMessage2[0] = y;
                try
                {
					aStringMessageReceiver2.sendResponseMessage(y.getResponseReceiverId(), "Response2");
				}
                catch (Exception err)
                {
                	EneterTrace.error("Sending Response2 failed.", err);
				}
            }
        });

        final StringResponseReceivedEventArgs[] aReceivedResponse1 = {null};
        aStringMessageSender1.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse1[0] = y;
            }
        });

        final StringResponseReceivedEventArgs[] aReceivedResponse2 = {null};
        aStringMessageSender2.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringResponseReceivedEventArgs y)
            {
                aReceivedResponse2[0] = y;
            }
        });

        aStringMessageSender1.sendMessage("Message1");

        assertEquals("Message1", aReceivedMessage1[0].getRequestMessage());//, "Message receiver 1 received incorrect message.");
        assertEquals("Response1", aReceivedResponse1[0].getResponseMessage());//, "Response receiver 1 received incorrect message.");
        assertNull(aReceivedMessage2[0]);//, "Message receiver 2 should not receive a message.");
        assertNull(aReceivedResponse2[0]);//, "Response receiver 2 should not receive a message.");


        aReceivedMessage1[0] = null;
        aReceivedResponse1[0] = null;

        aStringMessageSender2.sendMessage("Message2");

        assertEquals("Message2", aReceivedMessage2[0].getRequestMessage());//, "Message receiver 2 received incorrect message.");
        assertEquals("Response2", aReceivedResponse2[0].getResponseMessage());//, "Response receiver 2 received incorrect message.");
        assertNull(aReceivedMessage1[0]);//, "Message receiver 1 should not receive a message.");
        assertNull(aReceivedResponse1[0]);//, "Response receiver 1 should not receive a message.");
    }
    
    @Test
    public void AssociatedResponseReceiverId() throws Exception
    {
        // Wrapped/unwrapped channels
        String aChannel1Id = "Channel1Id";
        String aChannel2Id = "Channel2Id";

        IDuplexStringMessagesFactory aStringMessagesFactory = new DuplexStringMessagesFactory();

        IDuplexStringMessageReceiver aStringMessageReceiver1 = aStringMessagesFactory.createDuplexStringMessageReceiver();
        IDuplexStringMessageReceiver aStringMessageReceiver2 = aStringMessagesFactory.createDuplexStringMessageReceiver();

        IDuplexStringMessageSender aStringMessageSender1 = aStringMessagesFactory.createDuplexStringMessageSender();
        IDuplexStringMessageSender aStringMessageSender2 = aStringMessagesFactory.createDuplexStringMessageSender();

        // Attach input channels to string receivers.
        aStringMessageReceiver1.attachDuplexInputChannel(myLocalMessaging2.createDuplexInputChannel(aChannel1Id));
        aStringMessageReceiver2.attachDuplexInputChannel(myLocalMessaging2.createDuplexInputChannel(aChannel2Id));

        // Connect string senders with the channel wrapper.
        myDuplexChannelWrapper.attachDuplexInputChannel(myLocalMessaging1.createDuplexInputChannel(aChannel1Id));
        aStringMessageSender1.attachDuplexOutputChannel(myLocalMessaging1.createDuplexOutputChannel(aChannel1Id));
        
        myDuplexChannelWrapper.attachDuplexInputChannel(myLocalMessaging1.createDuplexInputChannel(aChannel2Id));
        aStringMessageSender2.attachDuplexOutputChannel(myLocalMessaging1.createDuplexOutputChannel(aChannel2Id));
        
        try
        {
            // Connect wrapper and unwrapper to global channels.
            myDuplexChannelUnwrapper.attachDuplexInputChannel(myDuplexGlobalInputChannel);
            myDuplexChannelWrapper.attachDuplexOutputChannel(myDuplexGlobalOutputChannel);
    
    
            final StringRequestReceivedEventArgs[] aReceivedMessage1 = { null };
            aStringMessageReceiver1.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
            {
                @Override
                public void onEvent(Object x, StringRequestReceivedEventArgs y)
                {
                    aReceivedMessage1[0] = y;
                }
            });
            
            final boolean[] aResponseReceiverChannel1Disconnected = { false };
            aStringMessageReceiver1.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
            {
                @Override
                public void onEvent(Object x, ResponseReceiverEventArgs y)
                {
                    aResponseReceiverChannel1Disconnected[0] = true;
                }
            });
            
            final StringRequestReceivedEventArgs[] aReceivedMessage2 = { null };
            aStringMessageReceiver2.requestReceived().subscribe(new EventHandler<StringRequestReceivedEventArgs>()
            {
                @Override
                public void onEvent(Object x, StringRequestReceivedEventArgs y)
                {
                    aReceivedMessage2[0] = y;
                }
            });
            
            final boolean[] aResponseReceiverChannel2Disconnected = { false };
            aStringMessageReceiver2.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
            {
                @Override
                public void onEvent(Object sender, ResponseReceiverEventArgs e)
                {
                    aResponseReceiverChannel2Disconnected[0] = true;
                }
            });
            
            aStringMessageSender1.sendMessage("Message1");
            aStringMessageSender2.sendMessage("Message2");
    
            String anAssociatedId1 = myDuplexChannelUnwrapper.getAssociatedResponseReceiverId(aReceivedMessage1[0].getResponseReceiverId());
            String anAssociatedId2 = myDuplexChannelUnwrapper.getAssociatedResponseReceiverId(aReceivedMessage2[0].getResponseReceiverId());
    
            assertEquals(anAssociatedId1, anAssociatedId2);
    
            myDuplexChannelUnwrapper.getAttachedDuplexInputChannel().disconnectResponseReceiver(anAssociatedId1);
            assertTrue(aResponseReceiverChannel1Disconnected[0]);
            assertTrue(aResponseReceiverChannel2Disconnected[0]);
        }
        finally
        {
            myDuplexChannelUnwrapper.detachDuplexInputChannel();
            myDuplexChannelWrapper.detachDuplexOutputChannel();
        }
    }

    private IMessagingSystemFactory myGlobalMessaging;
    private IMessagingSystemFactory myLocalMessaging1;
    private IMessagingSystemFactory myLocalMessaging2;
    
    private IDuplexChannelWrapper myDuplexChannelWrapper;
    private IDuplexChannelUnwrapper myDuplexChannelUnwrapper;

    private IDuplexOutputChannel myDuplexGlobalOutputChannel;
    private IDuplexInputChannel myDuplexGlobalInputChannel;
}
