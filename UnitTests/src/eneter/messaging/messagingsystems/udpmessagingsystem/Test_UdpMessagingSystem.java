package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.util.Random;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;


public class Test_UdpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new UdpMessagingSystemFactory();
        myChannelId = "udp://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
}
