package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import java.io.PrintStream;
import java.util.Random;

import org.junit.Before;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_BufferedMessaging_Tcp extends BufferedMessagingBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("d:\\EneterTrace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        ChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        
        TcpMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
        anUnderlyingMessaging.getClientSecurity().setConnectionTimeout(200);
        
        long aMaxOfflineTime = 1000;
        MessagingSystem = new BufferedMessagingFactory(anUnderlyingMessaging, aMaxOfflineTime);
        ConnectionInterruptionFrequency = 80;
    }
}
