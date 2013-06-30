package broker;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.nodes.broker.*;

public class Program
{

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        
        IDuplexBroker aBroker = new DuplexBrokerFactory().createBroker();
        aBroker.attachDuplexInputChannel(aMessaging.createDuplexInputChannel("tcp://127.0.0.1:7091/"));
        
        System.out.println("Broker is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        aBroker.detachDuplexInputChannel();
    }

}
