package eneter.messaging.nodes.dispatcher;

import static org.junit.Assert.*;

import org.junit.*;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Test_Dispatcher
{
    @Before
    public void setup() throws Exception
    {
        // Create channels
        IMessagingSystemFactory aMessagingSystemFactory = new SynchronousMessagingSystemFactory();
        myWritingChannel1 = aMessagingSystemFactory.createOutputChannel("Channel1");
        myReadingChannel1 = aMessagingSystemFactory.createInputChannel("Channel1");

        myWritingChannel2 = aMessagingSystemFactory.createOutputChannel("Channel2");
        myReadingChannel2 = aMessagingSystemFactory.createInputChannel("Channel2");

        myWritingChannel3 = aMessagingSystemFactory.createOutputChannel("Channel3");
        myReadingChannel3 = aMessagingSystemFactory.createInputChannel("Channel3");

        myWritingChannel4 = aMessagingSystemFactory.createOutputChannel("Channel4");
        myReadingChannel4 = aMessagingSystemFactory.createInputChannel("Channel4");

        // Create dispatcher
        IDispatcherFactory aDispatcherFactory = new DispatcherFactory();
        myDispatcher = aDispatcherFactory.createDispatcher();

        // Attach input channels
        myDispatcher.attachInputChannel(myReadingChannel1);
        myDispatcher.attachInputChannel(myReadingChannel2);

        // Attach ouptut channels
        myDispatcher.attachOutputChannel(myWritingChannel3);
        myDispatcher.attachOutputChannel(myWritingChannel4);
    }
    
    @Test
    public void dispatching() throws Exception
    {
        // Listen output from dispatcher
        final ChannelMessageEventArgs[] aReceivedFromChannel3 = {null};
        myReadingChannel3.messageReceived().subscribe(new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, ChannelMessageEventArgs y)
            {
                aReceivedFromChannel3[0] = y;
            }
        });
        myReadingChannel3.startListening();

        final ChannelMessageEventArgs[] aReceivedFromChannel4 = {null};
        myReadingChannel4.messageReceived().subscribe(new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, ChannelMessageEventArgs y)
            {
                aReceivedFromChannel4[0] = y;
            }
        });
        myReadingChannel4.startListening();

        // Send message to dispatcher via channel1
        myWritingChannel1.sendMessage("MyMessageA");

        // Check
        assertEquals("MyMessageA", aReceivedFromChannel3[0].getMessage());
        assertEquals("MyMessageA", aReceivedFromChannel4[0].getMessage());

        // Send message to dispatcher via channel2
        aReceivedFromChannel3[0] = null;
        aReceivedFromChannel4[0] = null;
        myWritingChannel2.sendMessage("MyMessageB");

        // Check
        assertEquals("MyMessageB", aReceivedFromChannel3[0].getMessage());
        assertEquals("MyMessageB", aReceivedFromChannel4[0].getMessage());
    }
    
    
    
    private IDispatcher myDispatcher;

    private IOutputChannel myWritingChannel1;
    private IInputChannel myReadingChannel1;

    private IOutputChannel myWritingChannel2;
    private IInputChannel myReadingChannel2;

    private IOutputChannel myWritingChannel3;
    private IInputChannel myReadingChannel3;

    private IOutputChannel myWritingChannel4;
    private IInputChannel myReadingChannel4;
}
