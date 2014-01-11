package eneter.messaging.endpoints.rpc;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_Rpc_Sync_Xml extends RpcBaseTester
{
    @Before
    public void setup()
    {
        mySerializer = new XmlStringSerializer();
        myChannelId = "channel_1";
        myMessaging = new SynchronousMessagingSystemFactory();
    }
}
