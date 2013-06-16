package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.util.Random;

import org.junit.*;

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
    
    @Ignore
    @Test
    @Override
    public void Oneway_06_StopListening()
    {
    }

    @Ignore
    @Test
    @Override
    public void Duplex_07_OpenConnection_if_InputChannelNotStarted()
    {
    }

    @Ignore
    @Test
    @Override
    public void Duplex_09_StopListening_SendMessage()
    {
    }
}
