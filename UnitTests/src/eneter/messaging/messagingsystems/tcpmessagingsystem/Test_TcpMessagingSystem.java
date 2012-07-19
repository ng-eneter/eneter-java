package eneter.messaging.messagingsystems.tcpmessagingsystem;

import static org.junit.Assert.*;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.ManualResetEvent;

public class Test_TcpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new TcpMessagingSystemFactory();
        myChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
    
    @Test(expected = SocketException.class)
    @Override
    public void A07_StopListening() throws Exception
    {
        super.A07_StopListening();
    }
    
    @Test
    public void sendReceiveMessage_5threads_10000messages() throws Exception
    {
        final IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(myChannelId);
        final IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(myChannelId);

        // Helping thread signaling end of message handling
        final ManualResetEvent anEverythingProcessedEvent = new ManualResetEvent(false);

        // Observe the input channel
        final ArrayList<String> aReceivedMessages = new ArrayList<String>();
        anInputChannel.messageReceived().subscribe(new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, ChannelMessageEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    aReceivedMessages.add((String)y.getMessage());

                    if (aReceivedMessages.size() == 2000 * 5)
                    {
                        anEverythingProcessedEvent.set();
                    }
                }
            }
        });
        
        try
        {
            // 5 competing threads
            ArrayList<Thread> aThreads = new ArrayList<Thread>();
            for (int i = 0; i < 5; ++i)
            {
                Thread aThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                     // Send messages
                        for (int ii = 0; ii < 2000; ++ii)
                        {
                            try
                            {
                                String aMessage = String.valueOf(ii);
                                anOutputChannel.sendMessage(aMessage);
                            } catch (Exception err)
                            {
                            }
                        }
                    }
                    
                });
                        
                aThreads.add(aThread);
            }

            anInputChannel.startListening();

            for (Thread x : aThreads)
            {
                x.start();
            }
            
            for (Thread x : aThreads)
            {
                x.join();
            }

            assertTrue(anEverythingProcessedEvent.waitOne(10000));//, "Timeout for processing of messages.");
        }
        finally
        {
            anInputChannel.stopListening();
        }

        // Check
        assertEquals(2000 * 5, aReceivedMessages.size());
    }
}
