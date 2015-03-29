package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import org.junit.*;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_MonitoredMessaging_Sync_Xml extends MonitoredMessagingTesterBase
{
    @Before
    public void setup()
    {
        EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        ChannelId = "ChannelId";
        mySerializer = new XmlStringSerializer();
        myUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        MessagingSystemFactory = new MonitoredMessagingFactory(myUnderlyingMessaging, mySerializer, 1000, 2000);
    }
}
