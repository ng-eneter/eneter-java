/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.HostListenerBase;
import eneter.net.system.*;
import eneter.net.system.linq.EnumerableExt;
import eneter.net.system.threading.ThreadPool;

class WebSocketHostListener extends HostListenerBase
{
    public WebSocketHostListener(InetSocketAddress address, IServerSecurityFactory securityFactory)
    {
        super(address, securityFactory);
    }
    
    
    /**
     * Handles connection for WebSocket protocol.
     * It reads the websocket protocol and forwards the incoming message according to the path
     * to the handler processing it.
     */
    @Override
    protected void handleConnection(Socket tcpClient)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Receive open websocket communication request.
                HashMap<String, String> aRegExResult = new HashMap<String, String>();
                HashMap<String, String> aHeaderFields = new HashMap<String, String>();
                WebSocketFormatter.decodeOpenConnectionHttpRequest(tcpClient.getInputStream(), aRegExResult, aHeaderFields);

                String aSecurityKey = aHeaderFields.get("Sec-WebSocket-Key");

                // If some required header field is missing or has incorrect value.
                if (!aHeaderFields.containsKey("Upgrade") ||
                    !aHeaderFields.containsKey("Connection") ||
                    StringExt.isNullOrEmpty(aSecurityKey))
                {
                    EneterTrace.warning(TracedObject() + "failed to receive open websocket connection request. (missing or incorrect header field)");
                    byte[] aCloseConnectionResponse = WebSocketFormatter.encodeCloseFrame(null, (short)400);
                    tcpClient.getOutputStream().write(aCloseConnectionResponse);
                    
                    return;
                }

                
                // Get the path to identify the end-point.
                String anIncomingPath = aRegExResult.get("path");

                // if the incoming path is the whole uri then extract the absolute path.
                String anAbsolutePath;
                try
                {
                    URI anIncomingUri = new URI(anIncomingPath);
                    anAbsolutePath = anIncomingUri.getPath();
                }
                catch (URISyntaxException err)
                {
                    anAbsolutePath = anIncomingPath;
                }

                // Get handler for that path.
                URI aHandlerUri;
                final IMethod1<Object> aPathHandler;
                synchronized (myHandlers)
                {
                    final String anAbsolutePathFinal = anAbsolutePath;
                    Entry<URI, IMethod1<Object>> aPair =
                            EnumerableExt.firstOrDefault(myHandlers,new IFunction1<Boolean, Entry<URI, IMethod1<Object>>>()
                                {
                                    @Override
                                    public Boolean invoke(
                                            Entry<URI, IMethod1<Object>> x)
                                            throws Exception
                                    {
                                        return x.getKey().getPath().equals(anAbsolutePathFinal);
                                    }
                                    
                                });
                    
                    aPathHandler = aPair.getValue(); 
                    aHandlerUri = aPair.getKey();
                }

                // If the listener does not exist.
                if (aPathHandler == null)
                {
                    EneterTrace.warning(TracedObject() + "does not listen to " + anIncomingPath);
                    byte[] aCloseConnectionResponse = WebSocketFormatter.encodeCloseFrame(null, (short)404);
                    tcpClient.getOutputStream().write(aCloseConnectionResponse);
                    
                    return;
                }

                // Response that the connection is accepted.
                byte[] anOpenConnectionResponse = WebSocketFormatter.encodeOpenConnectionHttpResponse(aSecurityKey);
                tcpClient.getOutputStream().write(anOpenConnectionResponse);


                // Create the context for connecting client.
                URI aClientContextUri = new URI(aHandlerUri.getScheme(), null, aHandlerUri.getHost(), getAddress().getPort(), anAbsolutePath, aRegExResult.get("query"), null);
                final WebSocketClientContext aClientContext = new WebSocketClientContext(aClientContextUri, aHeaderFields, tcpClient);


                // Call path handler in a another thread.
                ThreadPool.queueUserWorkItem(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                aPathHandler.invoke(aClientContext);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                    });

                
                // Start listening loop in the client context.
                // The loop will read websocket messages from the underlying tcp connection.
                // Note: User is responsible to call WebSocketClientContext.CloseConnection() to stop this loop
                //       or the service must close the conneciton.
                aClientContext.doRequestListening();
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to process TCP connection.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
   
    
    @Override
    protected String TracedObject()
    {
        return "WebSocketHostListener ";
    }
}
