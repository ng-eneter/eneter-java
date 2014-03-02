package eneter.messaging.endpoints.rpc;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.JavaBinarySerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_Rpc_Tcp_Bin extends RpcBaseTester
{
    @Before
    public void setup() throws FileNotFoundException
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        mySerializer = new JavaBinarySerializer();
        myChannelId = "tcp://127.0.0.1:8095/";
        myMessaging = new TcpMessagingSystemFactory();
    }
}
