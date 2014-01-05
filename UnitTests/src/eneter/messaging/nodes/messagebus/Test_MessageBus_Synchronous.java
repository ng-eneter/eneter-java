package eneter.messaging.nodes.messagebus;

import org.junit.After;
import org.junit.Before;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.nodes.broker.*;

public class Test_MessageBus_Synchronous extends MessageBusBaseTester
{
    @Before
    public void Setup() throws Exception
    {
        IMessagingSystemFactory anUnderlyingMessaging = new SynchronousMessagingSystemFactory();

        // Create the broker.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
        myBroker = aBrokerFactory.createBroker();
        myBroker.attachDuplexInputChannel(anUnderlyingMessaging.createDuplexInputChannel("BrokerAddress"));

        myMessagingSystemFactory = new MessageBusMessagingFactory("BrokerAddress", anUnderlyingMessaging);


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
