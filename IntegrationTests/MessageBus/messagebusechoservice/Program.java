package messagebusechoservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.endpoints.rpc.*;
import eneter.messaging.messagingsystems.composites.messagebus.MessageBusMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Program
{
    public static void main(String[] args) throws Exception
    {
        // The service will communicate via Message Bus which is listening via TCP.
        IMessagingSystemFactory aMessageBusUnderlyingMessaging = new TcpMessagingSystemFactory();
        // note: only TCP/IP address which is exposed for services is needed.
        IMessagingSystemFactory aMessaging = new MessageBusMessagingFactory("tcp://127.0.0.1:8045/", null, aMessageBusUnderlyingMessaging);
        
        // Create input channel listening via the message bus.
        // Note: this is address of the service inside the message bus.
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("Eneter.Echo");
        
        // Instantiate class implementing the service.
        IEcho anEcho = new EchoService();
        
        // Create the RPC service.
        IRpcService<IEcho> anEchoService = new RpcFactory().createSingleInstanceService(anEcho, IEcho.class);
        
        // Attach input channel to the service and start listening via the message bus.
        anEchoService.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Echo service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach the input channel and stop listening.
        anEchoService.detachDuplexInputChannel();
    }
}
