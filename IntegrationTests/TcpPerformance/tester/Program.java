package tester;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;

public class Program
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        System.out.printf("Test starts.\n");
        
        // TCP messaging.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        
        IDuplexInputChannel anInputChannel = null;
        IDuplexOutputChannel anOutputChannel = null;
        
        try
        {
            // Echo service.
            anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8094/");
            final IDuplexInputChannel anInputChannelFinal = anInputChannel;
            
            anInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
                {
                    try
                    {
                        // sends back the received message.
                        anInputChannelFinal.sendResponseMessage(e.getResponseReceiverId(), e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Echoing the response failed.", err);
                    }
                }
            });
    
            // Service starts listening.
            anInputChannel.startListening();
            System.out.printf("Echo service listening.\n");
            
            
            // Client.
            anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8094/");
            anOutputChannel.openConnection();
            
            final ManualResetEvent aResponsesReceived = new ManualResetEvent(false);
            final int[] aReceivedCount = { 0 };
            anOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
            {
                @Override
                public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
                {
                    ++aReceivedCount[0];
                    if (aReceivedCount[0] == 100000)
                    {
                        aResponsesReceived.set();
                    }
                }
            });
            
            System.out.printf("Client starts sending.\n");
            long aStartingTime = System.nanoTime();
            
            for (int i = 0; i < 100000; ++i)
            {
                anOutputChannel.sendMessage("Hello world.");
            }
            
            // Wait until all messages are received.
            if (!aResponsesReceived.waitOne(10000))
            {
                EneterTrace.error("Timeout.");
            }
            
            long anElapsedTime = System.nanoTime() - aStartingTime;
            System.out.printf("Time: %s\n", nanoToTime(anElapsedTime));
            
        }
        catch (Exception err)
        {
            EneterTrace.error("Performance test failed.\n", err);
        }
        
        if (anOutputChannel != null)
        {
            anOutputChannel.closeConnection();
        }
        
        if (anInputChannel != null)
        {
            anInputChannel.stopListening();
        }
        
        System.out.printf("Test ended.\n");
    }
    
    private static String nanoToTime(long elapsedTime)
    {
        long aHours = (long) (elapsedTime / (60.0 * 60.0 * 1000000000.0));
        elapsedTime -= aHours * 60 * 60 * 1000000000;
        
        long aMinutes = (long) (elapsedTime / (60.0 * 1000000000.0));
        elapsedTime -= aMinutes * 60 * 1000000000;
        
        long aSeconds = elapsedTime / 1000000000;
        elapsedTime -= aSeconds * 1000000000;
        
        long aMiliseconds = elapsedTime / 1000000;
        elapsedTime -= aMiliseconds * 1000000;
        
        double aMicroseconds = elapsedTime / 1000.0;

        return String.format("[%d:%d:%d %dms %.1fus]",
            aHours,
            aMinutes,
            aSeconds,
            aMiliseconds,
            aMicroseconds);
    }

}
