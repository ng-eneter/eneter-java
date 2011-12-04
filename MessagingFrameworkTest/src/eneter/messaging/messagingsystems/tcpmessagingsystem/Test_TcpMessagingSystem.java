package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.SocketException;

import org.junit.Before;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_TcpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        myMessagingSystemFactory = new TcpMessagingSystemFactory();
        myChannelId = "tcp://127.0.0.1:8091/";
    }
    
    @Test(expected = SocketException.class)
    @Override
    public void A07_StopListening() throws Exception
    {
        super.A07_StopListening();
    }
}
