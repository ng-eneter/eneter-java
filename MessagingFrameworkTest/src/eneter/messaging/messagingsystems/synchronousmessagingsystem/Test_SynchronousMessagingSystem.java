package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;

public class Test_SynchronousMessagingSystem extends MessagingSystemBaseTester
{
    @Before
    public void setup()
    {
        myMessagingSystemFactory = new SynchronousMessagingSystemFactory();
    }
}
