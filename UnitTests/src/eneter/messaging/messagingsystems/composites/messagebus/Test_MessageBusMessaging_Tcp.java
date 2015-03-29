package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.Random;

import org.junit.After;
import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_MessageBusMessaging_Tcp extends MessagingSystemBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Debug);
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));

        // Generate random number for the port.
        Random aRandomPort = new Random();
        int aPort1 = 7000 + aRandomPort.nextInt(1000);
        int aPort2 = aPort1 + 10;

        IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();

        IDuplexInputChannel aMessageBusServiceInputChannel = anUnderlyingMessaging.createDuplexInputChannel("tcp://[::1]:" + aPort1 + "/");
        IDuplexInputChannel aMessageBusClientInputChannel = anUnderlyingMessaging.createDuplexInputChannel("tcp://[::1]:" + aPort2 + "/");
        myMessageBus = new MessageBusFactory().createMessageBus();
        myMessageBus.attachDuplexInputChannels(aMessageBusServiceInputChannel, aMessageBusClientInputChannel);

        MessagingSystemFactory = new MessageBusMessagingFactory("tcp://[::1]:" + aPort1 + "/", "tcp://[::1]:" + aPort2 + "/", anUnderlyingMessaging);

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
