package eneter.messaging.endpoints.multitypedmessages;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_MultiTypedMessagesSyncXml extends MultiTypedMessagesBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();
        String aChannelId = "Channel1";
        ISerializer aSerializer = new XmlStringSerializer();

        Setup(aMessagingSystem, aChannelId, aSerializer);
    }
}
