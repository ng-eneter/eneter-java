package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.net.SocketException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_MonitoredMessaging_Tcp_Xml extends MonitoredMessagingTesterBase
{
    @Before
    public void setup()
    {
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        myChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        
        mySerializer = new XmlStringSerializer();
        myUnderlyingMessaging = new TcpMessagingSystemFactory();
        myMessagingSystemFactory = new MonitoredMessagingFactory(myUnderlyingMessaging, mySerializer, 1000, 2000);
    }
    
    @Test(expected = SocketException.class)
    @Override
    public void Oneway_06_StopListening() throws Exception
    {
        super.Oneway_06_StopListening();
    }
}
