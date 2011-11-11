package eneter.messaging.endpoints.stringmessages;

import org.junit.*;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.threadpoolmessagingsystem.ThreadPoolMessagingSystemFactory;

public class Test_StringMessages_ThreadPoolMessaging extends StringMessagesBaseTester
{
    @Before
    public void Setup()
    {
        IMessagingSystemFactory aMessagingSystem = new ThreadPoolMessagingSystemFactory();
        String aChannelId = "Channel1";

        setup(aMessagingSystem, aChannelId);
    }
}
