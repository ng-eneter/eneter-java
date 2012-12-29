package eneter.messaging.endpoints.typedmessages;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;

public class Test_TypedReliableMessages_Sync_JavaBinary extends TypedReliableMessagesBaseTester
{
    @Before
    public void setup() throws Exception
    {
        IMessagingSystemFactory aMessagingSystem = new SynchronousMessagingSystemFactory();

        String aChannelId = "Channel1";
        ISerializer aSerializer = new JavaBinarySerializer();

        setup(aMessagingSystem, aChannelId, aSerializer);
    }
}