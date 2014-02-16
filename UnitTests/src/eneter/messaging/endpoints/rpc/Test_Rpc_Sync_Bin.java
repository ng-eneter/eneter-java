package eneter.messaging.endpoints.rpc;

import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
    
    @Override
    @Ignore
    @Test(expected = TimeoutException.class)
    public void rpcTimeout() throws Exception
    {
        // Note: This test is not applicable for the synchronous messaging
        //       because synchronous messaging is a sequence within one thread and so the remote call
        //       does not wait.
    }
}
