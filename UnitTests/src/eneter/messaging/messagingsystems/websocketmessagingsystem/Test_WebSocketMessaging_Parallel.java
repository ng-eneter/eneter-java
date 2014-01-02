package eneter.messaging.messagingsystems.websocketmessagingsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.threading.dispatching.NoDispatching;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;

public class Test_WebSocketMessaging_Parallel extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new WebSocketMessagingSystemFactory()
            .setInputChannelThreading(new NoDispatching())
            .setInputChannelThreading(new NoDispatching());
        myChannelId = "ws://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
    
}
