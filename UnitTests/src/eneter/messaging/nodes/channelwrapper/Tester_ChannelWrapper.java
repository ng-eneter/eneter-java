package eneter.messaging.nodes.channelwrapper;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.endpoints.stringmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Tester_ChannelWrapper
{
    @Before
    public void setup() throws Exception
    {
        myGlobalMessagingSystem = new SynchronousMessagingSystemFactory();
        myGlobalOutputChannel = myGlobalMessagingSystem.createOutputChannel("MainChannel");
        myGlobalInputChannel = myGlobalMessagingSystem.createInputChannel("MainChannel");

        myLocalMessagingSystem1 = new SynchronousMessagingSystemFactory();
        myLocalMessagingSystem2 = new SynchronousMessagingSystemFactory();
        
        IChannelWrapperFactory aChannelWrapperFactory = new ChannelWrapperFactory();
        myChannelWrapper = aChannelWrapperFactory.createChannelWrapper();
        myChannelUnwrapper = aChannelWrapperFactory.createChannelUnwrapper(myLocalMessagingSystem2);
    }

    @Test
    public void WrapUnwrapChannels() throws Exception
    {
        // Wrapped/unwrapped channels
        String aChannel1Id = "Channel1Id";
        String aChannel2Id = "Channel2Id";
       
        IStringMessagesFactory aStringMessagesFactory = new StringMessagesFactory();

        // Create String senders and receivers
        IStringMessageSender aStringMessageSender1 = aStringMessagesFactory.CreateStringMessageSender();
        IStringMessageSender aStringMessageSender2 = aStringMessagesFactory.CreateStringMessageSender();

        IStringMessageReceiver aStringMessageReceiver1 = aStringMessagesFactory.CreateStringMessageReceiver();
        IStringMessageReceiver aStringMessageReceiver2 = aStringMessagesFactory.CreateStringMessageReceiver();

        // Sender side
        IInputChannel anInputChannel11 = myLocalMessagingSystem1.createInputChannel(aChannel1Id);
        IOutputChannel anOutputChannel11 = myLocalMessagingSystem1.createOutputChannel(aChannel1Id);
        
        IInputChannel anInputChannel12 = myLocalMessagingSystem1.createInputChannel(aChannel2Id);
        IOutputChannel anOutputChannel12 = myLocalMessagingSystem1.createOutputChannel(aChannel2Id);
        
        myChannelWrapper.attachInputChannel(anInputChannel11);
        aStringMessageSender1.attachOutputChannel(anOutputChannel11);
        
        myChannelWrapper.attachInputChannel(anInputChannel12);
        aStringMessageSender2.attachOutputChannel(anOutputChannel12);

        // Receiver side
        IInputChannel anInputChannel21 = myLocalMessagingSystem2.createInputChannel(aChannel1Id);
        IInputChannel anInputChannel22 = myLocalMessagingSystem2.createInputChannel(aChannel2Id);
        aStringMessageReceiver1.attachInputChannel(anInputChannel21);
        aStringMessageReceiver2.attachInputChannel(anInputChannel22);

        // Attach input output to the wrapper and unwrapper
        myChannelWrapper.attachOutputChannel(myGlobalOutputChannel);
        myChannelUnwrapper.attachInputChannel(myGlobalInputChannel);

        // Observing string message receivers
        final String[] aReceivedMessage1 = {""};

        //final String[] aReceivedMessage1 = {""};
        aStringMessageReceiver1.messageReceived().subscribe(new EventHandler<StringMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringMessageEventArgs y)
            {
                aReceivedMessage1[0] = y.getMessage();
            }
        });
        
        final String[] aReceivedMessage2 = {""};
        aStringMessageReceiver2.messageReceived().subscribe(new EventHandler<StringMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, StringMessageEventArgs y)
            {
                aReceivedMessage2[0] = y.getMessage();
            }
        });

        // Send the first message
        String aMessage1 = "Message1";
        aStringMessageSender1.sendMessage(aMessage1);

        // Check
        assertEquals(aMessage1, aReceivedMessage1[0]);
        assertEquals("", aReceivedMessage2[0]);

        // Send the second message
        aReceivedMessage1[0] = "";
        aReceivedMessage2[0] = "";

        String aMessage2 = "Message2";
        aStringMessageSender2.sendMessage(aMessage2);

        // Check
        assertEquals("", aReceivedMessage1[0]);
        assertEquals(aMessage2, aReceivedMessage2[0]);
    }
    
    
    private IMessagingSystemFactory myGlobalMessagingSystem;
    private IMessagingSystemFactory myLocalMessagingSystem1;
    private IMessagingSystemFactory myLocalMessagingSystem2;
    
    private IChannelWrapper myChannelWrapper;
    private IChannelUnwrapper myChannelUnwrapper;

    private IOutputChannel myGlobalOutputChannel;
    private IInputChannel myGlobalInputChannel;
}
