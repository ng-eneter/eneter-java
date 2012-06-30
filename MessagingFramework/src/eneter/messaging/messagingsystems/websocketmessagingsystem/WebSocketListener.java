/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
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
    /**
     * Construct websocket service.
     * @param webSocketUri service address. Provide port number too.
     */
    public WebSocketListener(URI webSocketUri)
    {
        this(webSocketUri, new NoneSecurityServerFactory());
    }
    
    /**
     * Construct websocket service.
     * @param webSocketUri service address. Provide port number too.
     * @param securityFactory Factory allowing SSL communication.
     */
    public WebSocketListener(URI webSocketUri, IServerSecurityFactory securityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAddress = webSocketUri;
            mySecurityFactory = securityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Starts listening.
     * To handle connected clients the connectionHandler is called. The connectionHandler handler
     * is called in parallel from multiple threads as clients are connected.
     * @param connectionHandler callback handler handling incoming connections. It is called from multiple threads.
     * @throws Exception
     */
    public void startListening(IMethod1<IWebSocketClientContext> connectionHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeningManipulatorLock)
                {
                    if (isListening())
                    {
                        String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                        EneterTrace.error(aMessage);
                        throw new IllegalStateException(aMessage);
                    }

                    if (connectionHandler == null)
                    {
                        throw new IllegalArgumentException("The input parameter connectionHandler is null.");
                    }

                    myConnectionHandler = connectionHandler;

                    WebSocketListenerController.startListening(myAddress, myConnectionHandler, mySecurityFactory);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Stops listening and closes all open connections with clients.
     */
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeningManipulatorLock)
                {
                    WebSocketListenerController.stopListening(myAddress);
                    myConnectionHandler = null;
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.StartListeningFailure, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the service is listening.
     * @return true if listening.
     * @throws Exception
     */
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return WebSocketListenerController.isListening(myAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns address of the service.
     */
    public URI getAddress()
    {
        return myAddress;
    }
    
    private URI myAddress;
    private IMethod1<IWebSocketClientContext> myConnectionHandler;
    private IServerSecurityFactory mySecurityFactory;
    private Object myListeningManipulatorLock = new Object();
    
    private String TracedObject()
    {
        return "WebSocketListener ";
    }
}
