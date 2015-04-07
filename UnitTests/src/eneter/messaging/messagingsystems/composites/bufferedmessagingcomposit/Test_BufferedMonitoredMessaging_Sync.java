package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.messagingsystems.composites.BufferedMonitoredMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_BufferedMonitoredMessaging_Sync extends BufferedMessagingBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        ChannelId = "Channel_1";
        IMessagingSystemFactory anUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        int aMaxOfflineTime = 1000;
        MessagingSystem = new BufferedMonitoredMessagingFactory(anUnderlyingMessaging, aMaxOfflineTime, 50, 50);
        ConnectionInterruptionFrequency = 5;
    }
}
