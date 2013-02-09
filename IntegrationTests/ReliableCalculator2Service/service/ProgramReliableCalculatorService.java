package service;

import java.io.*;

import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.websocketmessagingsystem.WebSocketMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class ProgramReliableCalculatorService
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
        // Create reliable message receiver.
        IReliableTypedMessagesFactory aReceiverFactory = new ReliableTypedMessagesFactory();
        IReliableTypedMessageReceiver<ResponseMessage, RequestMessage> aReceiver =
            aReceiverFactory.createReliableDuplexTypedMessageReceiver(ResponseMessage.class, RequestMessage.class);

        // Subscribe to be notified whether sent response messages
        // were received.
        aReceiver.responseMessageDelivered().subscribe(myOnResponseMessageDelivered);
        aReceiver.responseMessageNotDelivered().subscribe(myOnResponseMessageNotDelivered);

        // Subscribe to process request messages.
        aReceiver.messageReceived().subscribe(myOnRequestReceived);

        // Use WebSocket for the communication.
        // Note: You can also other messagings. E.g. TcpMessagingSystemFactory
        IMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("ws://127.0.0.1:8099/aaa/");

        // Attach the input channel to the receiver and start listening.
        aReceiver.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Calculator service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach the input channel to stop listening.
        aReceiver.detachDuplexInputChannel();
    }
    
    private static void onRequestReceived(Object sender, TypedRequestReceivedEventArgs<RequestMessage> e)
    {
        // Calculate numbers.
        ResponseMessage aResponseMessage = new ResponseMessage();
        aResponseMessage.Result = e.getRequestMessage().Number1 + e.getRequestMessage().Number2;

        System.out.println(e.getRequestMessage().Number1 + " + " + e.getRequestMessage().Number2 + " = " + Integer.toString(aResponseMessage.Result));

        // Send back the response message.
        IReliableTypedMessageReceiver<ResponseMessage, RequestMessage> aReceiver = (IReliableTypedMessageReceiver<ResponseMessage, RequestMessage>)sender;
        
        try
        {
            String aResponseId = aReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponseMessage);
            System.out.println("Sent response has Id: " + aResponseId);
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
    }

    private static void onResponseMessageDelivered(Object sender, ReliableMessageIdEventArgs e)
    {
        System.out.println("Response Id: " + e.getMessageId() + " was delivered.");
    }
    
    private static void onResponseMessageNotDelivered(Object sender, ReliableMessageIdEventArgs e)
    {
        System.out.println("Response Id: " + e.getMessageId() + " was NOT delivered.");
    }
    
    private static EventHandler<TypedRequestReceivedEventArgs<RequestMessage>> myOnRequestReceived = new EventHandler<TypedRequestReceivedEventArgs<RequestMessage>>()
    {
        @Override
        public void onEvent(Object sender,
                TypedRequestReceivedEventArgs<RequestMessage> e)
        {
            onRequestReceived(sender, e);
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
