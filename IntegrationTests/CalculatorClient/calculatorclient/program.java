package calculatorclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit.BufferedMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;


public class program
{
    public static class RequestMessage
    {
        public int Number1;
        public int Number2;
    }
    
    public static class ResponseMessage
    {
        public int Result;
    }

    public static void main(String[] args)
    {
        try
        {
            // Create the synchronous message sender.
            // It will wait max 5 seconds for the response.
            // To wait infinite time use TimeSpan.FromMiliseconds(-1) or
            // default constructor new DuplexTypedMessagesFactory()
            IDuplexTypedMessagesFactory aSenderFactory =
                new DuplexTypedMessagesFactory(25000);
            ISyncDuplexTypedMessageSender<ResponseMessage, RequestMessage> mySender = aSenderFactory.createSyncDuplexTypedMessageSender(ResponseMessage.class, RequestMessage.class);
    
            // Use Websocket for the communication.
            // If you want to use TCP then use TcpMessagingSystemFactory().
            //IMessagingSystemFactory aMessaging = new WebSocketMessagingSystemFactory();
            //IDuplexOutputChannel anOutputChannel =
            //    aMessaging.CreateDuplexOutputChannel("ws://127.0.0.1:8099/Calculator/");
            IMessagingSystemFactory aMessaging1 = new TcpMessagingSystemFactory();
            IMessagingSystemFactory aMessaging = new BufferedMessagingFactory(aMessaging1, 20000);
            IDuplexOutputChannel anOutputChannel =
                aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:4502/");
    
            // Attach the output channel and be able to send messages
            // and receive response messages.
            mySender.attachDuplexOutputChannel(anOutputChannel);
            
            
            RequestMessage aRequest = new RequestMessage();
            aRequest.Number1 = 10;
            aRequest.Number2 = 20;
            
            ResponseMessage aResponse = mySender.sendRequestMessage(aRequest);
            
            System.out.format("Result: %d", aResponse.Result);
            
            System.out.println("Restart the service.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            
            aRequest.Number1 = 1;
            aRequest.Number2 = 2;
            
            aResponse = mySender.sendRequestMessage(aRequest);
            
            System.out.format("Result: %d", aResponse.Result);
            
            mySender.detachDuplexOutputChannel();
            
        }
        catch (Exception err)
        {
            EneterTrace.error("Detected exception", err);
        }
    }

}
