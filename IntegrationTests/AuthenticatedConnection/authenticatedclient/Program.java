package authenticatedclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.stringmessages.*;
import eneter.messaging.messagingsystems.composites.authenticatedconnection.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Program
{
    private static class LoginProvider implements IGetLoginMessage
    {
        @Override
        public Object getLoginMessage(String channelId,
                String responseReceiverId)
        {
            return "John";
        }
    }
    
    private static class HandshakeResponseProvider implements IGetHandshakeResponseMessage
    {
        @Override
        public Object getHandshakeResponseMessage(String channelId,
                String responseReceiverId, Object handshakeMessage)
        {
            try
            {
                // Handshake response is encoded handshake message.
                ISerializer aSerializer = new AesSerializer("password1");
                Object aHandshakeResponse = aSerializer.serialize((String)handshakeMessage, String.class);
                
                return aHandshakeResponse;
            }
            catch (Exception err)
            {
                EneterTrace.warning("Processing handshake message failed. The connection will be closed.", err);
            }
            
            return null;
        }
    }
    
    private static EventHandler<StringResponseReceivedEventArgs> myOnResponseReceived = new EventHandler<StringResponseReceivedEventArgs>()
    {
        @Override
        public void onEvent(Object sender, StringResponseReceivedEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    private static IDuplexStringMessageSender mySender;
    
    public static void main(String[] args) throws Exception
    {
        // TCP messaging.
        IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
        
        // Authenticated messaging uses TCP as the underlying messaging.
        LoginProvider aLoginProvider = new LoginProvider();
        HandshakeResponseProvider aHandshakeResponseProvider = new HandshakeResponseProvider();
        IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aTcpMessaging, aLoginProvider, aHandshakeResponseProvider);
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8092/");
        
        // Use text messages.
        mySender = new DuplexStringMessagesFactory().createDuplexStringMessageSender();
        
        // Subscribe to receive response messages.
        mySender.responseReceived().subscribe(myOnResponseReceived);
        
        // Attach output channel and connect the service.
        mySender.attachDuplexOutputChannel(anOutputChannel);
        
        // Send a message.
        mySender.sendMessage("Hello");
        
        System.out.println("Client sent the message. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach output channel and stop listening.
        // Note: it releases the tread listening to responses.
        mySender.detachDuplexOutputChannel();
    }
    
    private static void onResponseMessageReceived(Object sender, StringResponseReceivedEventArgs e)
    {
        // Process the incoming response here.
        System.out.println(e.getResponseMessage());
    }

}
