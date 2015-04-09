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
            IProtocolFormatter aProtocol = new EasyProtocolFormatter();
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory(aProtocol);
            
            IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8033/");
        
            IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
            IMultiTypedMessageReceiver aReceiver = aFactory.createMultiTypedMessageReceiver();
            
            // Register message types which can be processed.
            aReceiver.registerRequestMessageReceiver(myStringHandler, String.class);
            aReceiver.registerRequestMessageReceiver(myMyRequestMessageHandler, MyRequestMessage.class);
            
            // Attach input channel and start listening.
            aReceiver.attachDuplexInputChannel(anInputChannel);
            
            System.out.println("Service is running. Press ENTER to stop.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            
            // Detach input channel to stop the listening thread.
            aReceiver.detachDuplexInputChannel();
            
            // Unregister message handlers.
            aReceiver.unregisterRequestMessageReceiver(String.class);
            aReceiver.unregisterRequestMessageReceiver(MyRequestMessage.class);
        }
        catch (Exception err)
        {
            EneterTrace.error("Service failed.", err);
        }
    }
    
    private static void onStringMessage(Object eventSender, TypedRequestReceivedEventArgs<String> e)
    {
        String aRequestMessage = e.getRequestMessage();
        System.out.println(aRequestMessage);
        
        // Send back the message.
        String aResponse = "Thanks for " + aRequestMessage;
        IMultiTypedMessageReceiver aReceiver = (IMultiTypedMessageReceiver)eventSender;
        
        try
        {
            aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponse, String.class);
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
    
    
    
    private static EventHandler<TypedRequestReceivedEventArgs<String>> myStringHandler =
            new EventHandler<TypedRequestReceivedEventArgs<String>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<String> e)
        {
            onStringMessage(sender, e);
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
