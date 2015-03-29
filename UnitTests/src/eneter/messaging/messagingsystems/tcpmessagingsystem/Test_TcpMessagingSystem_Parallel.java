package eneter.messaging.messagingsystems.tcpmessagingsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import helper.RandomPortGenerator;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.threading.dispatching.NoDispatching;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.ManualResetEvent;

public class Test_TcpMessagingSystem_Parallel extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new TcpMessagingSystemFactory()
        .setInputChannelThreading(new NoDispatching())
        .setOutputChannelThreading(new NoDispatching());
        
        ChannelId = "tcp://127.0.0.1:" + aPort + "/";
    }
    
}
