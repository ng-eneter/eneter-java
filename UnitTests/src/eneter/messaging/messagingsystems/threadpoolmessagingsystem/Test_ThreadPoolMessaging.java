package eneter.messaging.messagingsystems.threadpoolmessagingsystem;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_ThreadPoolMessaging extends MessagingSystemBaseTester
{
    @Before
    public void setup()
    {
        myMessagingSystemFactory = new ThreadPoolMessagingSystemFactory();
    }
}
