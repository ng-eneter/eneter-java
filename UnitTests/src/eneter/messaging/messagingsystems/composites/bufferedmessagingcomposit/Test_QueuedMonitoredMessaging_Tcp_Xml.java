package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.messagingsystems.composites.BufferedMonitoredMessagingFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_QueuedMonitoredMessaging_Tcp_Xml extends BufferedMessagingBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        ChannelId = "tcp://127.0.0.1:6070/";
        UnderlyingMessaging = new TcpMessagingSystemFactory();
        ISerializer aSerializer = new XmlStringSerializer();
        int aMaxOfflineTime = 1000;
        MessagingSystem = new BufferedMonitoredMessagingFactory(UnderlyingMessaging, aSerializer, aMaxOfflineTime, 200, 100);
        ConnectionInterruptionFrequency = 100;
    }
}
