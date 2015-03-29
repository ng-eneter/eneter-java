package eneter.messaging.messagingsystems.tcpmessagingsystem;

import static org.junit.Assert.*;

import helper.RandomPortGenerator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

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
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new TcpMessagingSystemFactory();
        
        //ChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        ChannelId = "tcp://[::1]:" + aPort + "/";
    }
    
    
}
