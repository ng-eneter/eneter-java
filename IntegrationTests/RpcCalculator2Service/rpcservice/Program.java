package rpcservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.endpoints.rpc.IRpcService;
import eneter.messaging.endpoints.rpc.RpcFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Program
{
    public static void main(String[] args) throws Exception
    {
        // Instantiating the calculator.
        Calculator aCalculator = new Calculator();
        
        // Exposing the calculator as a service.
        RpcFactory anRpcFactory = new RpcFactory();
        IRpcService<ICalculator> aService = anRpcFactory.createService(aCalculator, ICalculator.class);
        
        // Use TCP.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8045/");
        
        // Attach input channel and start listening.
        aService.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Calculator service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach input channel and stop listening.
        aService.detachDuplexInputChannel();
    }

}
