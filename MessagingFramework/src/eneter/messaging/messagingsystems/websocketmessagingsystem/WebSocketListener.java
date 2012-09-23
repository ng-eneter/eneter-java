/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal.PathListenerProviderBase;
import eneter.net.system.IMethod1;

/**
 * WebSocket server.
 * <br/>
 * The following example implements a simple service echoing the incoming message back to the client.
 * <pre>
 * {@code
 * import java.io.BufferedReader;
 * import java.io.InputStreamReader;
 * import java.net.URI;
 * 
 * import eneter.messaging.messagingsystems.websocketmessagingsystem.*;
 * import eneter.net.system.IMethod1;
 * 
 * public class Program
 * {
 *  public static void main(String[] args) throws Exception
 *    {
 *        WebSocketListener aService = new WebSocketListener(new URI("ws://127.0.0.1:8045/Echo/"));
 *        aService.startListening(new IMethod1<IWebSocketClientContext>()
 *            {
 *                // Method called if a client is connected.
 *                // The method is called is called in parallel for each connected client!
 *                public void invoke(IWebSocketClientContext client) throws Exception
 *                {
 *                    WebSocketMessage aMessage;
 *                    while ((aMessage = client.receiveMessage()) != null)
 *                    {
 *                        if (aMessage.isText())
 *                        {
 *                            String aTextMessage = aMessage.getWholeTextMessage();
 *                            
 *                            // Display the message.
 *                            System.out.println(aTextMessage);
 *                            
 *                            // Send back the echo.
 *                            client.sendMessage(aTextMessage);
 *                        }
 *                    }
 *                }
 *            });
 *        
 *        System.out.println("Websocket echo service is running. Press ENTER to stop.");
 *        new BufferedReader(new InputStreamReader(System.in)).readLine();
 *        
 *        aService.stopListening();
 *    }
 * }
 * }
 * </pre>
 *
 */
public class WebSocketListener
{
    private static class WebSocketListenerImpl extends PathListenerProviderBase
    {
        public WebSocketListenerImpl(URI webSocketUri)
        {
            super(new WebSocketHostListenerFactory(), webSocketUri, new NoneSecurityServerFactory());
        }
        
        public WebSocketListenerImpl(URI webSocketUri, IServerSecurityFactory securityFactory)
        {
            super(new WebSocketHostListenerFactory(), webSocketUri, securityFactory);
        }

        @Override
        protected String TracedObject()
        {
            return "WebSocketListener ";
        }
    }
    
    
    /**
     * Construct websocket service.
     * @param webSocketUri service address. Provide port number too.
     */
    public WebSocketListener(URI webSocketUri)
    {
        myListenerImpl = new WebSocketListenerImpl(webSocketUri);
    }
    
    /**
     * Construct websocket service.
     * @param webSocketUri service address. Provide port number too.
     * @param securityFactory Factory allowing SSL communication.
     */
    public WebSocketListener(URI webSocketUri, IServerSecurityFactory securityFactory)
    {
        myListenerImpl = new WebSocketListenerImpl(webSocketUri, securityFactory);
    }
    
    /**
     * Starts listening.
     * To handle connected clients the connectionHandler is called. The connectionHandler handler
     * is called in parallel from multiple threads as clients are connected.
     * @param connectionHandler callback handler handling incoming connections. It is called from multiple threads.
     * @throws Exception
     */
    public void startListening(final IMethod1<IWebSocketClientContext> connectionHandler) throws Exception
    {
        myListenerImpl.startListening(new IMethod1<Object>()
            {
                @Override
                public void invoke(Object t) throws Exception
                {
                    if (t instanceof IWebSocketClientContext)
                    {
                        connectionHandler.invoke((IWebSocketClientContext)t);
                    }
                }
            });
    }
    
    /**
     * Stops listening and closes all open connections with clients.
     */
    public void stopListening()
    {
        myListenerImpl.stopListening();
    }
    
    /**
     * Returns true if the service is listening.
     * @return true if listening.
     * @throws Exception
     */
    public boolean isListening() throws Exception
    {
        return myListenerImpl.isListening();
    }
    
    /**
     * Returns address of the service.
     */
    public URI getAddress()
    {
        return myListenerImpl.getAddress();
    }
    
    
    private WebSocketListenerImpl myListenerImpl;
}
