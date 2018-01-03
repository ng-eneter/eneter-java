package eneter.messaging.messagingsystems.websocketmessagingsystem;

import helper.EventWaitHandleExt;
import helper.RandomPortGenerator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketTimeoutException;

import org.junit.Before;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

public class Test_WebSocketMessaging extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new WebSocketMessagingSystemFactory();
        ChannelId = "ws://127.0.0.1:" + aPort + "/";
    }
    
    @Test(expected = SocketTimeoutException.class)
    public void ConnectionTimeout() throws Exception
    {
        WebSocketMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        aMessaging.getClientSecurity().setConnectionTimeout(1000);

        // Nobody is listening on this address.
        final IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("ws://109.74.151.135:8045/");

        final ManualResetEvent aConnectionCompleted = new ManualResetEvent(false);

        try
        {
            // Start opening in another thread to be able to measure
            // if the timeout occured with the specified time.
            final Exception[] anException = { null };
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        anOutputChannel.openConnection();
                    }
                    catch (Exception err)
                    {
                        anException[0] = err;

                    }
                    aConnectionCompleted.set();
                }
            });

            if (aConnectionCompleted.waitOne(1500))
            {
                throw anException[0];
            }
        }
        finally
        {
            anOutputChannel.closeConnection();
        }
    }
    
    @Test
    public void ClientReceiveTimeout() throws Exception
    {
        WebSocketMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        aMessaging.getClientSecurity().setReceiveTimeout(1000);

        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8046/");
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("ws://127.0.0.1:8046/");

        try
        {
            final ManualResetEvent aConnectionClosed = new ManualResetEvent(false);
            anOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelEventArgs e)
                {
                    EneterTrace.info("Connection closed.");
                    aConnectionClosed.set();
                }
            });
            
            anInputChannel.startListening();
            anOutputChannel.openConnection();

            EneterTrace.info("Connection opened.");

            // According to set receive timeout the client should get disconnected within 1 second.
            EventWaitHandleExt.waitIfNotDebugging(aConnectionClosed, 3000);
        }
        finally
        {
            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test
    public void ServiceReceiveTimeout() throws Exception
    {
        WebSocketMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        aMessaging.getServerSecurity().setReceiveTimeout(1000);

        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8046/");
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("ws://127.0.0.1:8046/");

        try
        {
            final ManualResetEvent aConnectionClosed = new ManualResetEvent(false);
            anInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
            {
                @Override
                public void onEvent(Object x, ResponseReceiverEventArgs y)
                {
                    EneterTrace.info("Response Receiver Disconnected: " + y.getResponseReceiverId());
                }
            });
            
            anOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelEventArgs e)
                {
                    EneterTrace.info("Connection closed.");
                    aConnectionClosed.set();
                }
            });

            anInputChannel.startListening();
            anOutputChannel.openConnection();

            EneterTrace.info("Connection opened: " + anOutputChannel.getResponseReceiverId());

            // According to set receive timeout the client should get disconnected within 1 second.
            EventWaitHandleExt.waitIfNotDebugging(aConnectionClosed, 3000);
        }
        finally
        {
            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test
    public void MaxAmountOfConnections() throws Exception
    {
        WebSocketMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        IServerSecurityFactory aSecurityFactory = aMessaging.getServerSecurity();
        aSecurityFactory.setMaxAmountOfConnections(2);
            
        IDuplexOutputChannel anOutputChannel1 = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel2 = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8049/");
        IDuplexOutputChannel anOutputChannel3 = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8049/");
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("ws://127.0.0.1:8049/");

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
            
            try
            {
                anOutputChannel3.openConnection();
            }
            catch (Exception err)
            {
                return;
            }

            fail("The third connection did not throw the expected exception.");
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
