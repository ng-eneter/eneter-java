package eneter.messaging.messagingsystems.tcpmessagingsystem;

import static org.junit.Assert.*;

import helper.RandomPortGenerator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;

public class Test_TcpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup() throws FileNotFoundException
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        EneterTrace.startProfiler();
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new TcpMessagingSystemFactory();
        
        //ChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        ChannelId = "tcp://[::1]:" + aPort + "/";
    }
    
    @After
    public void Clean()
    {
        EneterTrace.stopProfiler();
    }
    
    @Test
    public void TestAvailableIpAddresses() throws SocketException
    {
        System.out.println("Available IP addresses:");
        String[] anAvailableIpAddresses = TcpMessagingSystemFactory.getAvailableIpAddresses();
        for (String anIpAddress : anAvailableIpAddresses)
        {
            System.out.println(anIpAddress);
        }
    }
    
    
    @Test
    public void MaxAmountOfConnections() throws Exception
    {
        TcpMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IServerSecurityFactory aSecurityFactory = aMessaging.getServerSecurity();
        aSecurityFactory.setMaxAmountOfConnections(2);
            
        IDuplexOutputChannel anOutputChannel1 = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel2 = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel3 = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8049/");
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8049/");

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
