package rpcclient;

import eneter.messaging.endpoints.rpc.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventArgs;
import eneter.net.system.EventHandler;

public class Program
{
    // Event handler
    private static EventHandler<EventArgs> myOnSomethingHappened = new EventHandler<EventArgs>()
    {
        @Override
        public void onEvent(Object sender, EventArgs e)
        {
            onSomethingHappened(sender, e);
        }
    };
    
    public static void main(String[] args) throws Exception
    {
        // Create the client.
        IRpcFactory anRpcFactory = new RpcFactory();
        IRpcClient<ICalculator> aClient = anRpcFactory.createClient(ICalculator.class);
        
        // Use TCP.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8045/");
        
        // Attach the output channel and connect the service.
        aClient.attachDuplexOutputChannel(anOutputChannel);
        
        
        // Get the service proxy.
        ICalculator aCalculatorProxy = aClient.getProxy();
        
        // Subscribe the event.
        aCalculatorProxy.somethingHappened().subscribe(myOnSomethingHappened);
        
        double aResult = aCalculatorProxy.Sum(10.0, 30.0);
        
        System.out.println("Result = " + Double.toString(aResult));
        
        
        // Unsubscribe the event.
        aCalculatorProxy.somethingHappened().unsubscribe(myOnSomethingHappened);
        
        // Disconnect from the service.
        aClient.detachDuplexOutputChannel();
    }
    
    private static void onSomethingHappened(Object sender, EventArgs e)
    {
        // Handle the event from the service here.
    }

}
