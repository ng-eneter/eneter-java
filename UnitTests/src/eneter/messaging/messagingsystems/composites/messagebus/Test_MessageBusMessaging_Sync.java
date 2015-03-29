package eneter.messaging.messagingsystems.composites.messagebus;

import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_MessageBusMessaging_Sync extends MessagingSystemBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Debug);
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));

        IMessagingSystemFactory anUnderlyingMessaging = new SynchronousMessagingSystemFactory();

        IDuplexInputChannel aMessageBusServiceInputChannel = anUnderlyingMessaging.createDuplexInputChannel("MyServicesAddress");
        IDuplexInputChannel aMessageBusClientInputChannel = anUnderlyingMessaging.createDuplexInputChannel("MyClientsAddress");
        myMessageBus = new MessageBusFactory().createMessageBus();
        myMessageBus.attachDuplexInputChannels(aMessageBusServiceInputChannel, aMessageBusClientInputChannel);

        MessagingSystemFactory = new MessageBusMessagingFactory("MyServicesAddress", "MyClientsAddress", anUnderlyingMessaging);

        // Address of the service in the message bus.
        ChannelId = "Service1_Address";
    }
    
    @After
    public void tearDown()
    {
        if (myMessageBus != null)
        {
            myMessageBus.detachDuplexInputChannels();
            myMessageBus = null;
        }
    }
    
    
    private IMessageBus myMessageBus;
}
