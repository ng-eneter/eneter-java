package multitypedservice;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Program
{
    public static class MyRequestMessage
    {
        public double Number1;
        public double Number2;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            // Create multi-typed receiver.
            IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
            IMultiTypedMessageReceiver aReceiver = aFactory.createMultiTypedMessageReceiver();
            
            // Register message types which can be processed.
            aReceiver.registerRequestMessageReceiver(myIntegerHandler, Integer.class);
            aReceiver.registerRequestMessageReceiver(myMyRequestMessageHandler, MyRequestMessage.class);
            
            // Attach input channel and start listening e.g. using TCP.
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8033/");
            aReceiver.attachDuplexInputChannel(anInputChannel);
            
            System.out.println("Service is running. Press ENTER to stop.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            
            // Detach input channel to stop the listening thread.
            aReceiver.detachDuplexInputChannel();
        }
        catch (Exception err)
        {
            EneterTrace.error("Service failed.", err);
        }
    }
    
    private static void onIntegerMessage(Object eventSender, TypedRequestReceivedEventArgs<Integer> e)
    {
        int aNumber = e.getRequestMessage();
        
        // Calculate factorial.
        int aResult = 1;
        for (int i = 1; i <= aNumber; ++i)
        {
            aResult *= i;
        }
        
        System.out.println(aNumber + "! =" + aResult);
        
        // Send back the result.
        IMultiTypedMessageReceiver aReceiver = (IMultiTypedMessageReceiver)eventSender;
        try
        {
            aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult, Integer.class);
        }
        catch (Exception err)
        {
            EneterTrace.error("Failed to send the response message.", err);
        }
    }
    
    private static void onMyReqestMessage(Object eventSender, TypedRequestReceivedEventArgs<MyRequestMessage> e)
    {
        MyRequestMessage aRequestMessage = e.getRequestMessage();
        
        double aResult = aRequestMessage.Number1 + aRequestMessage.Number2;
        
        System.out.println(aRequestMessage.Number1 + " + " + aRequestMessage.Number2 + " = " + aResult);
        
        // Send back the message.
        IMultiTypedMessageReceiver aReceiver = (IMultiTypedMessageReceiver)eventSender;
        try
        {
            aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult, Double.class);
        }
        catch (Exception err)
        {
            EneterTrace.error("Failed to send the response message.", err);
        }
    }
    
    
    
    private static EventHandler<TypedRequestReceivedEventArgs<Integer>> myIntegerHandler =
            new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<Integer> e)
        {
            onIntegerMessage(sender, e);
        }
    };
    
    private static EventHandler<TypedRequestReceivedEventArgs<MyRequestMessage>> myMyRequestMessageHandler =
            new EventHandler<TypedRequestReceivedEventArgs<MyRequestMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<MyRequestMessage> e)
        {
            onMyReqestMessage(sender, e);
        }
    };

}
