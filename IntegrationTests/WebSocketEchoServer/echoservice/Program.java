package echoservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import eneter.messaging.messagingsystems.websocketmessagingsystem.*;
import eneter.net.system.IMethod1;

public class Program
{
    public static void main(String[] args) throws Exception
    {
        WebSocketListener aService = new WebSocketListener(new URI("ws://127.0.0.1:8045/Echo/"));
        aService.startListening(new IMethod1<IWebSocketClientContext>()
            {
                // Method called if a client is connected.
                // The method is called is called in parallel for each connected client!
                @Override
                public void invoke(IWebSocketClientContext client) throws Exception
                {
                    WebSocketMessage aMessage;
                    while ((aMessage = client.receiveMessage()) != null)
                    {
                        if (aMessage.isText())
                        {
                            String aTextMessage = aMessage.getWholeTextMessage();
                            
                            // Display the message.
                            System.out.println(aTextMessage);
                            
                            // Send back the echo.
                            client.sendMessage(aTextMessage);
                        }
                    }
                }
            });
        
        System.out.println("Websocket echo service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        aService.stopListening();
    }
}
