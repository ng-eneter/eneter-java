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
        UdpMessagingSystemFactory aMessaging = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
        .setUnicastCommunication(false)
        .setAllowSendingBroadcasts(true);
        //.setReuseAddress(true);
        
        UdpMessagingSystemFactory aMessaging2 = new UdpMessagingSystemFactory(new EasyProtocolFormatter())
                .setUnicastCommunication(false)
                //.setAllowReceivingBroadcasts(true)
                .setReuseAddress(true);

        final ManualResetEvent aMessage1Received = new ManualResetEvent(false);
        final String[] aReceivedMessage1 = {null};
        IDuplexInputChannel anInputChannel1 = aMessaging2.createDuplexInputChannel("udp://127.0.0.1:8095/");
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
        IDuplexInputChannel anInputChannel2 = aMessaging2.createDuplexInputChannel("udp://127.0.0.1:8095/");
        anInputChannel2.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage2[0] = (String)y.getMessage();
                aMessage2Received.set();
            }
        });

        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("udp://255.255.255.255:8095/", "udp://127.0.0.1");

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
}
