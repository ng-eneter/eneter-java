package authenticatedservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.stringmessages.*;
import eneter.messaging.messagingsystems.composites.authenticatedconnection.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Program
{
    private static class HandshakeProvider implements IGetHandshakeMessage
    {
        @Override
        public Object getHandshakeMessage(String channelId, String responseReceiverId, Object loginMessage)
        {
            // Check if login is ok.
            if (loginMessage instanceof String)
            {
                String aLoginName = (String) loginMessage;
                if (myUsers.containsKey(aLoginName))
                {
                    // Login is OK so generate the handshake message.
                    // e.g. generate GUI.
                    return UUID.randomUUID().toString();
                }
            }
            
            // Login was not ok so there is not handshake message
            // and the connection will be closed.
            EneterTrace.warning("Login was not ok. The connection will be closed.");
            return null;
        }
    }
    
    private static class AuthenticateProvider implements IAuthenticate
    {
        @Override
        public boolean authenticate(String channelId,
                String responseReceiverId, Object loginMessage,
                Object handshakeMessage, Object handshakeResponseMessage)
        {
            if (loginMessage instanceof String)
            {
                // Get the password associated with the user.
                String aLoginName = (String) loginMessage;
                String aPassword = myUsers.get(aLoginName);
                
                // E.g. handshake response may be encrypted original handshake message.
                //      So decrypt incoming handshake response and check if it is equal to original handshake message.
                try
                {
                    ISerializer aSerializer = new AesSerializer(aPassword);
                    String aDecodedHandshakeResponse = aSerializer.deserialize(handshakeResponseMessage, String.class);
                    String anOriginalHandshake = (String) handshakeMessage;
                    if (anOriginalHandshake.equals(aDecodedHandshakeResponse))
                    {
                        // The handshake response is correct so the connection can be established.
                        return true;
                    }
                }
                catch (Exception err)
                {
                    // Decoding of the response message failed.
                    // The authentication will not pass.
                    EneterTrace.warning("Decoding handshake message failed.", err);
                }
            }
            
            // Authentication did not pass.
            EneterTrace.warning("Authentication did not pass. The connection will be closed.");
            return false;
        }
    }
    
    // Helper class subscribing to receive incoming messages.
    private static EventHandler<StringRequestReceivedEventArgs> myOnRequestReceived = new EventHandler<StringRequestReceivedEventArgs>()
    {
        @Override
        public void onEvent(Object sender, StringRequestReceivedEventArgs e)
        {
            onRequestReceived(sender, e);
        }
    };
    
    // [login, password]
    private static HashMap<String, String> myUsers = new HashMap<String, String>();
    private static IDuplexStringMessageReceiver myReceiver;
    
    public static void main(String[] args) throws Exception
    {
        // Simulate users.
        myUsers.put("John", "password1");
        myUsers.put("Steve", "password2");
        
        // TCP messaging.
        IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
        
        // Authenticated messaging uses TCP as the underlying messaging.
        HandshakeProvider aHandshakeProvider = new HandshakeProvider();
        AuthenticateProvider anAuthenticationProvider = new AuthenticateProvider();
        IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aTcpMessaging, aHandshakeProvider, anAuthenticationProvider);
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8092/");
        
        // Use just simple text messages.
        myReceiver = new DuplexStringMessagesFactory().createDuplexStringMessageReceiver();
        
        // Subscribe to receive messages.
        myReceiver.requestReceived().subscribe(myOnRequestReceived);
        
        // Attach input channel and start listening.
        // Note: using AuthenticatedMessaging will ensure the connection will be established only
        //       if the authentication procedure passes.
        myReceiver.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach input channel and stop listening.
        // Note: tis will release the listening thread.
        myReceiver.detachDuplexInputChannel();
    }
    
    private static void onRequestReceived(Object sender, StringRequestReceivedEventArgs e)
    {
        // Process the incoming message here.
        System.out.println(e.getRequestMessage());
        
        // Send back the response.
        try
        {
            myReceiver.sendResponseMessage(e.getResponseReceiverId(), "hello");
        }
        catch (Exception err)
        {
            // Sending of response message failed.
            // e.g. if the client disconnected meanwhile.
            EneterTrace.error("Sending of response failed.", err);
        }
    }
}
