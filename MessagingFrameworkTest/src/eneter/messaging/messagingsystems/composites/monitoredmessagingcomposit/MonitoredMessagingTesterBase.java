package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;
import eneter.net.system.threading.*;


public abstract class MonitoredMessagingTesterBase extends MessagingSystemBaseTester
{
    public void A09_OpenCloseConnection()
    {
        // This test-case is not applicable, because the output channel sends the ping and that will reconnect the connection.
    }

    public void A15_DuplexOutputChannelConnected_CloseFromOpenHandler()
    {
        // This test-case is not applicable, because the output channel sends the ping and that will reconnect the connection.
    }
    
    @Test
    public void B01_Pinging_StopListening() throws Exception
    {
        IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aDisconnectedEvent = new AutoResetEvent(false);

        final boolean[] aDisconnectedFlag = {false};
        aDuplexOutputChannel.connectionClosed().subscribe(new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2) throws Exception
            {
                aDisconnectedFlag[0] = true;
                aDisconnectedEvent.set();
            }
        });

        try
        {
            // Start listening.
            aDuplexInputChannel.startListening();

            // Start pinging and wait 5 seconds.
            aDuplexOutputChannel.openConnection();
            Thread.sleep(5000);

            assertFalse(aDisconnectedFlag[0]);

            // Stop listener, therefore the ping response will not come and the channel should indicate the disconnection.
            aDuplexInputChannel.stopListening();

            assertTrue(aDisconnectedEvent.waitOne(60000));

            assertTrue(aDisconnectedFlag[0]);
            assertFalse(aDuplexOutputChannel.isConnected());
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    @Test
    public void B02_Pinging_DisconnectResponseReceiver() throws Exception
    {
        IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aDisconnectedEvent = new AutoResetEvent(false);

        final boolean[] aDisconnectedFlag = {false};
        aDuplexOutputChannel.connectionClosed().subscribe(new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2) throws Exception
            {
                aDisconnectedFlag[0] = true;
                aDisconnectedEvent.set();
            }
        });

        try
        {
            // Start listening.
            aDuplexInputChannel.startListening();

            // Allow some time for pinging.
            aDuplexOutputChannel.openConnection();
            Thread.sleep(2000);

            assertFalse(aDisconnectedFlag[0]);

            // Disconnect the duplex output channel.
            aDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());

            aDisconnectedEvent.waitOne();

            assertTrue(aDisconnectedFlag[0]);
            assertFalse(aDuplexOutputChannel.isConnected());
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    @Test
    public void B03_Pinging_NoResponseForPing() throws Exception
    {
        // Create mock for the monitor duplex input channel.
        IDuplexInputChannel anUnderlyingDuplexInputChannel = myUnderlyingMessaging.createDuplexInputChannel(myChannelId);
        Mock_MonitorDuplexInputChannel aDuplexInputChannel = new Mock_MonitorDuplexInputChannel(anUnderlyingDuplexInputChannel, mySerializer);
        
        IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aDisconnectedEvent = new AutoResetEvent(false);

        final boolean[] aDisconnectedFlag = {false};
        aDuplexOutputChannel.connectionClosed().subscribe(new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2) throws Exception
            {
                aDisconnectedFlag[0] = true;
                aDisconnectedEvent.set();
            }
        });

        try
        {
            // Start listening.
            aDuplexInputChannel.startListening();

            // Allow some time for pinging.
            aDuplexOutputChannel.openConnection();
            Thread.sleep(5000);

            EneterTrace.info("B03_Pinging_NoResponseForPing() turned off responding for Ping.");
            assertFalse(aDisconnectedFlag[0]);

            // Turn off the responding on pings.
            aDuplexInputChannel.myResponsePingFlag = false;

            assertTrue(aDisconnectedEvent.waitOne(60000));

            assertTrue(aDisconnectedFlag[0]);
            assertFalse(aDuplexOutputChannel.isConnected());
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    @Test
    public void B04_Pinging_CloseConnection() throws Exception
    {
        IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        
        final String[] aClosedResponseReceiverId = {""};
        aDuplexInputChannel.responseReceiverDisconnected().subscribe(new IMethod2<Object, ResponseReceiverEventArgs>()
        {
            @Override
            public void invoke(Object x, ResponseReceiverEventArgs y)
                    throws Exception
            {
                aClosedResponseReceiverId[0] = y.getResponseReceiverId();
                aConnectionClosedEvent.set();
            }
        });

        try
        {
            // Start listening.
            aDuplexInputChannel.startListening();

            // Allow some time for pinging.
            aDuplexOutputChannel.openConnection();
            Thread.sleep(2000);

            assertEquals("", aClosedResponseReceiverId[0]);

            // Close connection. Therefore the duplex input channel will not get any pings anymore.
            aDuplexOutputChannel.closeConnection();

            assertTrue(aConnectionClosedEvent.waitOne(60000));

            assertEquals(aDuplexOutputChannel.getResponseReceiverId(), aClosedResponseReceiverId[0]);
            assertFalse(aDuplexOutputChannel.isConnected());
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    protected ISerializer mySerializer;
    protected IMessagingSystemFactory myUnderlyingMessaging;
}
