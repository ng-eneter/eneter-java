package eneter.messaging.messagingsystems.udpmessagingsystem;

import helper.RandomPortGenerator;

import java.util.Random;

import org.junit.*;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;


public class Test_UdpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new UdpMessagingSystemFactory();
        ChannelId = "udp://127.0.0.1:" + aPort + "/";
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
    
    @Ignore
    @Test
    @Override
    public void Duplex_03_Send1_10MB()
    {
    }
}
