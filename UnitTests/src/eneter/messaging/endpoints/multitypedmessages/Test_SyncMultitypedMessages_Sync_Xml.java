package eneter.messaging.endpoints.multitypedmessages;

import org.junit.Before;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_SyncMultitypedMessages_Sync_Xml extends SyncMultiTypedMessageBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;

        IMessagingSystemFactory aMessaging = new SynchronousMessagingSystemFactory();
        InputChannel = aMessaging.createDuplexInputChannel("MyChannelId");
        OutputChannel = aMessaging.createDuplexOutputChannel("MyChannelId");

        MultiTypedMessagesFactory = new eneter.messaging.endpoints.typedmessages.MultiTypedMessagesFactory(); 
    }
}
