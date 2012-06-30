package eneter.messaging.endpoints.stringmessages;

import org.junit.*;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.threadmessagingsystem.ThreadMessagingSystemFactory;

public class Test_StringMessages_ThreadMessaging extends StringMessagesBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory aMessagingSystem = new ThreadMessagingSystemFactory();
        String aChannelId = "Channel1";

        setup(aMessagingSystem, aChannelId);
    }
}
