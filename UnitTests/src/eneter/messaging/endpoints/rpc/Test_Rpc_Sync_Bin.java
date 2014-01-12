package eneter.messaging.endpoints.rpc;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_Rpc_Sync_Bin extends RpcBaseTester
{
    @Before
    public void setup()
    {
        mySerializer = new JavaBinarySerializer();
        myChannelId = "channel_1";
        myMessaging = new SynchronousMessagingSystemFactory();
    }
}
