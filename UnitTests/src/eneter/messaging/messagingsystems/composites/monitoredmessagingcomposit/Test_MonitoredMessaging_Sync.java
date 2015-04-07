package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import org.junit.*;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_MonitoredMessaging_Sync extends MonitoredMessagingTesterBase
{
    @Before
    public void setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        ChannelId = "ChannelId";
        myUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        MessagingSystemFactory = new MonitoredMessagingFactory(myUnderlyingMessaging, 1000, 2000);
    }
}
