package eneter.messaging.endpoints.rpc;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.JavaBinarySerializer;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_Rpc_Tcp_Bin extends RpcBaseTester
{
    @Before
    public void setup()
    {
        mySerializer = new JavaBinarySerializer();
        myChannelId = "tcp://127.0.0.1:8095/";
        myMessaging = new TcpMessagingSystemFactory();
    }
}
