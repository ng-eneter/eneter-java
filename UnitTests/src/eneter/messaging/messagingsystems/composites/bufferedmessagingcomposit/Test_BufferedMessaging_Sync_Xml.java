package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_BufferedMessaging_Sync_Xml extends BufferedMessagingBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("d:\\EneterTrace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        
        ChannelId = "Channel_1";
        UnderlyingMessaging = new SynchronousMessagingSystemFactory();
        long aMaxOfflineTime = 1000;
        MessagingSystem = new BufferedMessagingFactory(UnderlyingMessaging, aMaxOfflineTime);
        ConnectionInterruptionFrequency = 5;
    }
}
