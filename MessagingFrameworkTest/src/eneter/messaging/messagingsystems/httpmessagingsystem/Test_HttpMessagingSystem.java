package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.util.Random;

import org.junit.Before;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_HttpMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        myMessagingSystemFactory = new HttpMessagingSystemFactory();
        myChannelId = "http://127.0.0.1:" + Integer.toString(aPort) + "/Testing/";
    }
}
