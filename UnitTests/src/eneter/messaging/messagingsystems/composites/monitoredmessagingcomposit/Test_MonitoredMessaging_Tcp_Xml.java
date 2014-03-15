package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_MonitoredMessaging_Tcp_Xml extends MonitoredMessagingTesterBase
{
    @Before
    public void setup() throws FileNotFoundException
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setNameSpaceFilter(Pattern.compile("^eneter.messaging.messagingsystems.composites.*"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        myChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        
        mySerializer = new XmlStringSerializer();
        myUnderlyingMessaging = new TcpMessagingSystemFactory();
        myMessagingSystemFactory = new MonitoredMessagingFactory(myUnderlyingMessaging, mySerializer, 1000, 2000);
        
    }
    
    @Ignore
    @Test
    @Override
    public void Duplex_04_Send50000() throws Exception
    {
    }
    
}
