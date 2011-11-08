package eneter.messaging.messagingsystems.threadmessagingsystem;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_ThreadMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void setup()
    {
        myMessagingSystemFactory = new ThreadMessagingSystemFactory();
    }
}
