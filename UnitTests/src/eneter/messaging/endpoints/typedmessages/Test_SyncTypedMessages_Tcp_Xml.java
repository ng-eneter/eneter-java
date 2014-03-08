package eneter.messaging.endpoints.typedmessages;

import org.junit.Before;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_SyncTypedMessages_Tcp_Xml extends SyncTypedMessagesBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        InputChannel = aMessaging.createDuplexInputChannel("tcp://localhost:8034/");
        OutputChannel = aMessaging.createDuplexOutputChannel("tcp://localhost:8034/");

        DuplexTypedMessagesFactory = new DuplexTypedMessagesFactory();
    }
}
