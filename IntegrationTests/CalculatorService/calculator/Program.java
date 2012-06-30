package calculator;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.EventHandler;

public class Program
{
    public static class MyRequestMsg
    {
        public double Number1;
        public double Number2;
    }
    
    public static class MyResponseMsg
    {
        public double Result;
    }
    
    // Receiver receiving MyResponseMsg and responding MyRequestMsg
    private static IDuplexTypedMessageReceiver<MyResponseMsg, MyRequestMsg> myReceiver;

    public static void main(String[] args) throws Exception
    {
        // Start the TCP Policy server.
        // Note: Silverlight requests the policy xml to check if the connection
        //       can be established.
        TcpPolicyServer aPolicyServer = new TcpPolicyServer();
        aPolicyServer.startPolicyServer();
        
        // Create receiver that receives MyRequestMsg and
        // responses MyResponseMsg
        IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory();
        myReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(MyResponseMsg.class, MyRequestMsg.class);
        
        // Subscribe to handle incoming messages.
        myReceiver.messageReceived().subscribe(myOnMessageReceived);
        
        // Create input channel listening to TCP.
        // Note: Silverlight can communicate only on ports: 4502 - 4532
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:4502/");

        // Attach the input channel to the receiver and start the listening.
        myReceiver.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Calculator service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach the duplex input channel and stop the listening.
        // Note: it releases the thread listening to messages.
        myReceiver.detachDuplexInputChannel();
        
        // Stop the TCP policy server.
        aPolicyServer.stopPolicyServer();
    }
    
    private static void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<MyRequestMsg> e)
    {
        // Calculate incoming numbers.
        double aResult = e.getRequestMessage().Number1 + e.getRequestMessage().Number2;
        
        System.out.println(e.getRequestMessage().Number1 + " + " + e.getRequestMessage().Number2 + " = " + aResult);
        
        // Response back the result.
        MyResponseMsg aResponseMsg = new MyResponseMsg();
        aResponseMsg.Result = aResult;
        try
        {
            myReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMsg);
        }
        catch (Exception err)
        {
            EneterTrace.error("Sending the response message failed.", err);
        }
    }
    

    // Handler used to subscribe for incoming messages.
    private static EventHandler<TypedRequestReceivedEventArgs<MyRequestMsg>> myOnMessageReceived
            = new EventHandler<TypedRequestReceivedEventArgs<MyRequestMsg>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<MyRequestMsg> e)
        {
            onMessageReceived(sender, e);
        }
    };
}
