package messagebusechoclient;

import messagebusechoservice.IEcho;
import eneter.messaging.endpoints.rpc.IRpcClient;
import eneter.messaging.endpoints.rpc.RpcFactory;
import eneter.messaging.messagingsystems.composites.messagebus.MessageBusMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Program
{
    public static void main(String[] args) throws Exception
    {
        // The client will communicate via Message Bus which is listening via TCP.
        IMessagingSystemFactory aMessageBusUnderlyingMessaging = new TcpMessagingSystemFactory();
        // note: only TCP/IP address which is exposed for clients is needed. 
        IMessagingSystemFactory aMessaging = new MessageBusMessagingFactory(null, "tcp://127.0.0.1:8046/", aMessageBusUnderlyingMessaging);
        
        // Create output channel that will connect the service via the message bus..
        // Note: this is address of the service inside the message bus.
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("Eneter.Echo");
        
        // Create the RPC client for the Echo Service.
        IRpcClient<IEcho> aClient = new RpcFactory().createClient(IEcho.class);
        
        // Attach the output channel and be able to communicate with the service via the message bus.
        aClient.attachDuplexOutputChannel(anOutputChannel);
        
        // Get the service proxy and call the echo method.
        IEcho aProxy = aClient.getProxy();
        String aResponse = aProxy.hello("hello");
        
        System.out.println("Echo service returned: " + aResponse);
        
        // Detach the output channel.
        aClient.detachDuplexOutputChannel();
    }

}
