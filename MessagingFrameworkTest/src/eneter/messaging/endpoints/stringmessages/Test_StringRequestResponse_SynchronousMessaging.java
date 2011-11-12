package eneter.messaging.endpoints.stringmessages;

import org.junit.Before;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_StringRequestResponse_SynchronousMessaging extends StringRequestResponseBaseTester
{
    @Before
    public void setup()
    {
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        String aChannelId = "Channel1";

        setup(aMessagingSystem, aChannelId);
    }
}
