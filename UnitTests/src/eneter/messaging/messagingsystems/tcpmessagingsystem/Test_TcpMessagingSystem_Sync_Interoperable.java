package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import helper.*;

import org.junit.Before;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.connectionprotocols.EasyProtocolFormatter;

public class Test_TcpMessagingSystem_Sync_Interoperable extends MessagingSystemBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        //EneterTrace.StartProfiler();

        // Generate random number for the port.
        String aPort = RandomPortGenerator.generate();

        MessagingSystemFactory = new TcpMessagingSystemFactory(new EasyProtocolFormatter());
        //ChannelId = "tcp://127.0.0.1:" + aPort + "/";
        ChannelId = "tcp://[::1]:" + aPort + "/";

        this.CompareResponseReceiverId = false;
        this.myRequestMessage = new byte[] { (byte)'M', (byte)'E', (byte)'S', (byte)'S', (byte)'A', (byte)'G', (byte)'E' };
        this.myResponseMessage = new byte[] { (byte)'R', (byte)'E', (byte)'S', (byte)'P', (byte)'O', (byte)'N', (byte)'S', (byte)'E' };
        this.myMessage_10MB = RandomDataGenerator.getBytes(10000000);
    }
}
