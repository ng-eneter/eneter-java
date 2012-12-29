package eneter.messaging.endpoints.typedmessages;

import org.junit.Before;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_SyncTypedMessages_Sync_Xml extends SyncTypedMessagesBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory aMessaging = new SynchronousMessagingSystemFactory();
        InputChannel = aMessaging.createDuplexInputChannel("MyChannelId");
        OutputChannel = aMessaging.createDuplexOutputChannel("MyChannelId");

        SyncTypedMessagesFactory = new SyncTypedMessagesFactory();
        DuplexTypedMessagesFactory = new DuplexTypedMessagesFactory();
    }
}
