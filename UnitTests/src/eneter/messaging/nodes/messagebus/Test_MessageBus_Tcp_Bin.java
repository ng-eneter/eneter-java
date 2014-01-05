package eneter.messaging.nodes.messagebus;

import org.junit.After;
import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.nodes.broker.*;

public class Test_MessageBus_Tcp_Bin extends MessageBusBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();

        ISerializer aSerializer = new JavaBinarySerializer();

        // Create the broker.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory(aSerializer);
        myBroker = aBrokerFactory.createBroker();
        myBroker.attachDuplexInputChannel(anUnderlyingMessaging.createDuplexInputChannel("tcp://127.0.0.1:8034/"));

        myMessagingSystemFactory = new MessageBusMessagingFactory("tcp://127.0.0.1:8034/", anUnderlyingMessaging, aSerializer);


        myChannelId = "Service1_Address";
    }
    
    @After
    public void TearDown()
    {
        if (myBroker != null)
        {
            myBroker.detachDuplexInputChannel();
            myBroker = null;
        }
    }
    
    
    private IDuplexBroker myBroker;
}
