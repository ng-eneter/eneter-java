package eneter.messaging.endpoints.stringmessages;

import org.junit.*;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_StringMessages_SynchronousMessaging extends StringMessagesBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        String aChannelId = "Channel1";

        setup(aMessagingSystem, aChannelId);
    }
}
