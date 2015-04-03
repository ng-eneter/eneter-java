package eneter.messaging.messagingsystems.threadmessagingsystem;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.junit.Before;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_ThreadMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        MessagingSystemFactory = new ThreadMessagingSystemFactory();
    }
}
