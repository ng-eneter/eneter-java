package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.PrintStream;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_MonitoredMessaging_Sync extends MonitoredMessagingTesterBase
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        ChannelId = "ChannelId";
        myUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        MessagingSystemFactory = new MonitoredMessagingFactory(myUnderlyingMessaging, 250, 750);
    }
}
