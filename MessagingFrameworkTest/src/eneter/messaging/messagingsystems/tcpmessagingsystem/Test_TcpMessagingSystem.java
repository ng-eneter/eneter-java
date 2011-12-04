package eneter.messaging.messagingsystems.tcpmessagingsystem;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_TcpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        myMessagingSystemFactory = new TcpMessagingSystemFactory();
        myChannelId = "tcp://127.0.0.1:8091/";
    }
}
