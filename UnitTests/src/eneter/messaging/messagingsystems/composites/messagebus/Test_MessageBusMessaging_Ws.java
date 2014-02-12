package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.Random;

import org.junit.After;
import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.websocketmessagingsystem.WebSocketMessagingSystemFactory;

public class Test_MessageBusMessaging_Ws extends MessagingSystemBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Debug);
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));

        // Generate random number for the port.
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        

        IMessagingSystemFactory anUnderlyingMessaging = new WebSocketMessagingSystemFactory();

        IDuplexInputChannel aMessageBusServiceInputChannel = anUnderlyingMessaging.createDuplexInputChannel("ws://[::1]:" + aPort + "/Clients/");
        IDuplexInputChannel aMessageBusClientInputChannel = anUnderlyingMessaging.createDuplexInputChannel("ws://[::1]:" + (aPort + 10) + "/Services/");
        myMessageBus = new MessageBusFactory().createMessageBus();
        myMessageBus.attachDuplexInputChannels(aMessageBusServiceInputChannel, aMessageBusClientInputChannel);

        myMessagingSystemFactory = new MessageBusMessagingFactory("ws://[::1]:" + aPort + "/Clients/", "ws://[::1]:" + (aPort + 10) + "/Services/", anUnderlyingMessaging);

        // Address of the service in the message bus.
        myChannelId = "Service1_Address";
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
