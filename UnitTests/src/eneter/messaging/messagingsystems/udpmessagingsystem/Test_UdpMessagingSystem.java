package eneter.messaging.messagingsystems.udpmessagingsystem;

import helper.RandomPortGenerator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;


public class Test_UdpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new UdpMessagingSystemFactory();
        ChannelId = "udp://127.0.0.1:" + aPort + "/";
    }
    

    @Ignore
    @Test
    @Override
    public void Duplex_07_OpenConnection_if_InputChannelNotStarted()
    {
    }

    @Ignore
    @Test
    @Override
    public void Duplex_09_StopListening_SendMessage()
    {
    }
    
    @Ignore
    @Test
    @Override
    public void Duplex_03_Send1_10MB()
    {
    }
    
    @Test
    public void MaxAmountOfConnections() throws Exception
    {
        UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory();
        aMessaging.setMaxAmountOfConnections(2);
            
        IDuplexOutputChannel anOutputChannel1 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel2 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel3 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8049/");
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8049/");

        try
        {
            final ManualResetEvent aConnectionClosed = new ManualResetEvent(false);
            anOutputChannel3.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelEventArgs e)
                {
                    EneterTrace.info("Connection closed.");
                    aConnectionClosed.set();
                }
            });
            

            anInputChannel.startListening();
            anOutputChannel1.openConnection();
            anOutputChannel2.openConnection();
            anOutputChannel3.openConnection();

            if (!aConnectionClosed.waitOne(1000))
            {
                fail("Third connection was not closed.");
            }

            assertTrue(anOutputChannel1.isConnected());
            assertTrue(anOutputChannel2.isConnected());
            assertFalse(anOutputChannel3.isConnected());
        }
        finally
        {
            anOutputChannel1.closeConnection();
            anOutputChannel2.closeConnection();
            anOutputChannel3.closeConnection();
            anInputChannel.stopListening();
        }
    }
}
