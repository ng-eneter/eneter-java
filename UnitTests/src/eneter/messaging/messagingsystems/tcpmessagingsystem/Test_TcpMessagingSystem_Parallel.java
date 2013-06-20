package eneter.messaging.messagingsystems.tcpmessagingsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.ChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.EConcurrencyMode;
import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;

public class Test_TcpMessagingSystem_Parallel extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new TcpMessagingSystemFactory(EConcurrencyMode.ConcurrentConnections);
        myChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
    
    @Ignore
    @Test
    @Override
    public void Duplex_03_Send100_10MB() throws Exception
    {
        super.Duplex_03_Send100_10MB();
    }
    
    @Test(expected = SocketException.class)
    @Override
    public void Oneway_06_StopListening() throws Exception
    {
        super.Oneway_06_StopListening();
    }
}
