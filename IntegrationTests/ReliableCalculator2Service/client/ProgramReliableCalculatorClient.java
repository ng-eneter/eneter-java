package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.websocketmessagingsystem.*;
import eneter.net.system.EventHandler;

public class ProgramReliableCalculatorClient
{
    // Request message.
    public static class RequestMessage
    {
        public int Number1;
        public int Number2;
    }

    // Response message.
    public static class ResponseMessage
    {
        public int Result;
    }
    
    public static void main(String[] args) throws Exception
    {
        // Create the message sender.
        IReliableTypedMessagesFactory aSenderFactory = new ReliableTypedMessagesFactory();
        IReliableTypedMessageSender<ResponseMessage, RequestMessage> aSender 
            = aSenderFactory.createReliableDuplexTypedMessageSender(ResponseMessage.class, RequestMessage.class);

        // Subscribe to be notified whether the request message was delivered or not.
        aSender.messageDelivered().subscribe(myOnResponseMessageDelivered);
        aSender.messageNotDelivered().subscribe(myOnResponseMessageNotDelivered);

        // Subscribe to receive response messages.
        aSender.responseReceived().subscribe(myOnResponseReceived);

        // Use Websocket for the communication.
        // If you want to use TCP then use TcpMessagingSystemFactory().
        IMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("ws://127.0.0.1:8099/aaa/");

        // Attach the output channel and be able to send messages
        // and receive response messages.
        aSender.attachDuplexOutputChannel(anOutputChannel);
        
        Scanner aReader = new Scanner(System.in);
        
        RequestMessage aRequestMessage = new RequestMessage();
        
        // Ask user to put values.
        System.out.println("Number1 = ");
        aRequestMessage.Number1 = aReader.nextInt();
        
        System.out.println("Number2 = ");
        aRequestMessage.Number2 = aReader.nextInt();
        
        // Send the request to calculate given numbers.
        String aMessageId = aSender.sendRequestMessage(aRequestMessage);
        System.out.println("Sent request has id: " + aMessageId);
        
        System.out.println("Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        aSender.detachDuplexOutputChannel();
    }
    
    private static void onResponseReceived(Object sender, TypedResponseReceivedEventArgs<ResponseMessage> e)
    {
        System.out.println("Result =  " + Integer.toString(e.getResponseMessage().Result));
    }

    private static void onResponseMessageDelivered(Object sender, ReliableMessageIdEventArgs e)
    {
        System.out.println("Response Id: " + e.getMessageId() + " was delivered.");
    }
    
    private static void onResponseMessageNotDelivered(Object sender, ReliableMessageIdEventArgs e)
    {
        System.out.println("Response Id: " + e.getMessageId() + " was NOT delivered.");
    }
    
    
    private static EventHandler<TypedResponseReceivedEventArgs<ResponseMessage>> myOnResponseReceived = new EventHandler<TypedResponseReceivedEventArgs<ResponseMessage>>()
        {
            @Override
            public void onEvent(Object sender,
                    TypedResponseReceivedEventArgs<ResponseMessage> e)
            {
                onResponseReceived(sender, e);
            }
        };
    
    private static EventHandler<ReliableMessageIdEventArgs> myOnResponseMessageDelivered = new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object sender, ReliableMessageIdEventArgs e)
            {
                onResponseMessageDelivered(myOnResponseMessageDelivered, e);
            }
        };
        
    private static EventHandler<ReliableMessageIdEventArgs> myOnResponseMessageNotDelivered = new EventHandler<ReliableMessageIdEventArgs>()
        {
            @Override
            public void onEvent(Object sender, ReliableMessageIdEventArgs e)
            {
                onResponseMessageNotDelivered(myOnResponseMessageDelivered, e);
            }
        };
}
