package eneter.messaging.messagingsystems.udpmessagingsystem;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.messagingsystems.connectionprotocols.EasyProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;
import helper.EventWaitHandleExt;

public class Test_UdpMulticastAndBroadcast
{
    @Test
    public void BroadcastFromClientToAllServices() throws Exception
    {
        UdpMessagingSystemFactory anOutputChannelMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
            .setUnicastCommunication(false)
            .setAllowSendingBroadcasts(true);
        
        UdpMessagingSystemFactory anInputChannelMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
            .setUnicastCommunication(false)
            .setReuseAddress(true);

        final ManualResetEvent aMessage1Received = new ManualResetEvent(false);
        final String[] aReceivedMessage1 = {null};
        IDuplexInputChannel anInputChannel1 = anInputChannelMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
        anInputChannel1.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage1[0] = (String)y.getMessage();
                aMessage1Received.set();
            }
        });
        
        final ManualResetEvent aMessage2Received = new ManualResetEvent(false);
        final String[] aReceivedMessage2 = {null};
        IDuplexInputChannel anInputChannel2 = anInputChannelMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
        anInputChannel2.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage2[0] = (String)y.getMessage();
                aMessage2Received.set();
            }
        });

        IDuplexOutputChannel anOutputChannel = anOutputChannelMessaging.createDuplexOutputChannel("udp://255.255.255.255:8095/", "udp://127.0.0.1");

        try
        {
            anInputChannel1.startListening();
            anInputChannel2.startListening();

            anOutputChannel.openConnection();
            anOutputChannel.sendMessage("Hello");

            EventWaitHandleExt.waitIfNotDebugging(aMessage1Received, 1000);
            EventWaitHandleExt.waitIfNotDebugging(aMessage2Received, 1000);
        }
        finally
        {
            anOutputChannel.closeConnection();

            anInputChannel1.stopListening();
            anInputChannel2.stopListening();
        }

        assertEquals("Hello", aReceivedMessage1[0]);
        assertEquals("Hello", aReceivedMessage2[0]);
    }
    
    @Test
    public void BroadcastFromServiceToAllClients() throws Exception
    {
        UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
            .setUnicastCommunication(false)
            .setAllowSendingBroadcasts(true)
            .setReuseAddress(true);

        final ManualResetEvent aMessage1Received = new ManualResetEvent(false);
        final String[] aReceivedMessage1 = {null};
        IDuplexOutputChannel anOutputChannel1 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8090/", "udp://127.0.0.1:8092/");
        anOutputChannel1.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage1[0] = (String)y.getMessage();
                aMessage1Received.set();
            }
        });

        final ManualResetEvent aMessage2Received = new ManualResetEvent(false);
        final String[] aReceivedMessage2 = {null};
        IDuplexOutputChannel anOutputChannel2 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8090/", "udp://127.0.0.1:8092/");
        anOutputChannel2.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage2[0] = (String)y.getMessage();
                aMessage2Received.set();
            }
        });
        

        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8090/");

        try
        {
            anInputChannel.startListening();

            anOutputChannel1.openConnection();
            anOutputChannel2.openConnection();

            anInputChannel.sendResponseMessage("udp://255.255.255.255:8092/", "Hello");

            EventWaitHandleExt.waitIfNotDebugging(aMessage1Received, 1000);
            EventWaitHandleExt.waitIfNotDebugging(aMessage2Received, 1000);
        }
        finally
        {
            anInputChannel.stopListening();

            anOutputChannel1.closeConnection();
            anOutputChannel2.closeConnection();
        }

        assertEquals("Hello", aReceivedMessage1[0]);
        assertEquals("Hello", aReceivedMessage2[0]);
    }
    
    @Test
    public void MulticastFromClientToServices() throws Exception
    {
        UdpMessagingSystemFactory anInputChannelMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
            .setUnicastCommunication(false)
            .setReuseAddress(true)
            .setMulticastLoopback(true)
            .setMulticastGroupToReceive("234.1.2.3");
        
        UdpMessagingSystemFactory anOutputChannelMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
                .setUnicastCommunication(false);

        final ManualResetEvent aMessage1Received = new ManualResetEvent(false);
        final String[] aReceivedMessage1 = {null};
        IDuplexInputChannel anInputChannel1 = anInputChannelMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
        anInputChannel1.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage1[0] = (String)y.getMessage();
                aMessage1Received.set();
            }
        });

        final ManualResetEvent aMessage2Received = new ManualResetEvent(false);
        final String[] aReceivedMessage2 = {null};
        IDuplexInputChannel anInputChannel2 = anInputChannelMessaging.createDuplexInputChannel("udp://127.0.0.1:8095/");
        anInputChannel2.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage2[0] = (String)y.getMessage();
                aMessage2Received.set();
            }
        });
        
        IDuplexOutputChannel anOutputChannel = anOutputChannelMessaging.createDuplexOutputChannel("udp://234.1.2.3:8095/", "udp://127.0.0.1:8096/");

        try
        {
            anInputChannel1.startListening();
            anInputChannel2.startListening();

            Thread.sleep(1000);

            anOutputChannel.openConnection();

            anOutputChannel.sendMessage("Hello");

            EventWaitHandleExt.waitIfNotDebugging(aMessage1Received, 3000);
            EventWaitHandleExt.waitIfNotDebugging(aMessage2Received, 3000);
        }
        finally
        {
            anOutputChannel.closeConnection();

            anInputChannel1.stopListening();
            anInputChannel2.stopListening();
        }

        assertEquals("Hello", aReceivedMessage1[0]);
        assertEquals("Hello", aReceivedMessage2[0]);
    }
    
    @Test
    public void MulticastFromServiceToClients() throws Exception
    {
        UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
                .setUnicastCommunication(false)
                .setReuseAddress(true)
                .setMulticastGroupToReceive("234.1.2.3")
                .setMulticastLoopback(true);
        

        final ManualResetEvent aMessage1Received = new ManualResetEvent(false);
        final String[] aReceivedMessage1 = {null};
        IDuplexOutputChannel anOutputChannel1 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8090/", "udp://127.0.0.1:8092/");
        anOutputChannel1.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage1[0] = (String)y.getMessage();
                aMessage1Received.set();
            }
        });

        final ManualResetEvent aMessage2Received = new ManualResetEvent(false);
        final String[] aReceivedMessage2 = {null};
        IDuplexOutputChannel anOutputChannel2 = aMessaging.createDuplexOutputChannel("udp://127.0.0.1:8090/", "udp://127.0.0.1:8092/");
        anOutputChannel2.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage2[0] = (String)y.getMessage();
                aMessage2Received.set();
            }
        });

        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("udp://127.0.0.1:8090/");

        try
        {
            anInputChannel.startListening();

            anOutputChannel1.openConnection();
            anOutputChannel2.openConnection();

            anInputChannel.sendResponseMessage("udp://234.1.2.3:8092/", "Hello");

            EventWaitHandleExt.waitIfNotDebugging(aMessage1Received, 3000);
            EventWaitHandleExt.waitIfNotDebugging(aMessage2Received, 3000);
        }
        finally
        {
            anInputChannel.stopListening();

            anOutputChannel1.closeConnection();
            anOutputChannel2.closeConnection();
        }

        assertEquals("Hello", aReceivedMessage1[0]);
        assertEquals("Hello", aReceivedMessage2[0]);
    }
    
    @Test
    public void MulticastLoopback() throws Exception
    {
        UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
                .setUnicastCommunication(false)
                .setMulticastGroupToReceive("234.1.2.3")
                .setMulticastLoopback(true);

        final ManualResetEvent aMessageReceived = new ManualResetEvent(false);
        final String[] aReceivedMessage = {null};
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://234.1.2.3:8090/", "udp://0.0.0.0:8090/");
        anOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage[0] = (String)y.getMessage();
                aMessageReceived.set();
            }
        });

        try
        {
            anOutputChannel.openConnection();
            anOutputChannel.sendMessage("Hello");
            EventWaitHandleExt.waitIfNotDebugging(aMessageReceived, 3000);
        }
        finally
        {
            anOutputChannel.closeConnection();
        }

        assertEquals("Hello", aReceivedMessage[0]);
    }
}
