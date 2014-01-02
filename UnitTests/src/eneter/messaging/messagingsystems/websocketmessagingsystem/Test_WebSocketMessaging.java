package eneter.messaging.messagingsystems.websocketmessagingsystem;

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
import eneter.net.system.threading.internal.ManualResetEvent;

public class Test_WebSocketMessaging extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new WebSocketMessagingSystemFactory();
        myChannelId = "ws://127.0.0.1:" + Integer.toString(aPort) + "/";
    }
    
}
