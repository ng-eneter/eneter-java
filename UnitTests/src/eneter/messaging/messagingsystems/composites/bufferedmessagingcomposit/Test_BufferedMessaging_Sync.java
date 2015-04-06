package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Ignore;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_BufferedMessaging_Sync extends BufferedMessagingBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("d:\\EneterTrace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        ChannelId = "Channel_1";
        IMessagingSystemFactory anUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        long aMaxOfflineTime = 1000;
        MessagingSystem = new BufferedMessagingFactory(anUnderlyingMessaging, aMaxOfflineTime);
        ConnectionInterruptionFrequency = 5;
    }
}
