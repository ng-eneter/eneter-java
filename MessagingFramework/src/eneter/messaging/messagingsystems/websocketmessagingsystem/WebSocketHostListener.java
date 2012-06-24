package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.HashSetExt;
import eneter.net.system.linq.EnumerableExt;
import eneter.net.system.threading.ThreadPool;

class WebSocketHostListener
{
    public WebSocketHostListener(InetSocketAddress address, IServerSecurityFactory securityFactory)
    {
        myAddress = address;
        myTcpListener = new TcpListenerProvider(address, securityFactory);
    }
    
    public void registerListener(URI address, IMethod1<IWebSocketClientContext> processConnection)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlers)
            {
                // If the path listener already exists then error, because only one instance can listen.
                if (isAnyHandler(address))
                {
                    // The listener already exists.
                    String anErrorMessage = TracedObject() + "detected the address is already used.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }


                // Add handler for this path.
                Entry<URI, IMethod1<IWebSocketClientContext>> aHandler = new AbstractMap.SimpleEntry<URI, IMethod1<IWebSocketClientContext>>(address, processConnection);
                myHandlers.add(aHandler);

                // If the host listener does not listen to sockets yet, then start it.
                if (myTcpListener.isListening() == false)
                {
                    try
                    {
                        myTcpListener.startListening(myHandleConnectionHandler);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to start the path listener.", err);

                        unregisterListener(address);

                        throw err;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void unregisterListener(final URI address)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myHandlers)
                {
                    // Remove handler for that path.
                    HashSetExt.removeWhere(myHandlers, new IFunction1<Boolean, Entry<URI, IMethod1<IWebSocketClientContext>>>()
                        {
                            @Override
                            public Boolean invoke(
                                    Entry<URI, IMethod1<IWebSocketClientContext>> x)
                                    throws Exception
                            {
                                return x.getKey().getPath().equals(address.getPath());
                            }
                        });
                    
                
                    // If there is no the end point then nobody is handling messages and the listening can be stopped.
                    if (myHandlers.isEmpty())
                    {
                        myTcpListener.stopListening();
                    }
                }
            }
            catch (Exception err)
            {
                String anErrorMessage = TracedObject() + "failed to unregister path-listener.";
                EneterTrace.warning(anErrorMessage, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean existListener(URI address) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlers)
            {
                boolean isAny = isAnyHandler(address);
                return isAny;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public boolean existAnyListener()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlers)
            {
                return myHandlers.size() > 0;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleConnection(Socket tcpClient)
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
                final IMethod1<IWebSocketClientContext> aPathHandler;
                synchronized (myHandlers)
                {
                    final String anAbsolutePathFinal = anAbsolutePath;
                    Entry<URI, IMethod1<IWebSocketClientContext>> aPair =
                            EnumerableExt.firstOrDefault(myHandlers, new IFunction1<Boolean, Entry<URI, IMethod1<IWebSocketClientContext>>>()
                                {
                                    @Override
                                    public Boolean invoke(
                                            Entry<URI, IMethod1<IWebSocketClientContext>> x)
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


                // Create the context for conecting client.
                URI aClientContextUri = new URI(aHandlerUri.getScheme(), "", aHandlerUri.getHost(), myAddress.getPort(), anAbsolutePath, aRegExResult.get("Query"), "");
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
    
    
    private boolean isAnyHandler(final URI address) throws Exception
    {
        boolean isAny = EnumerableExt.any(myHandlers, new IFunction1<Boolean, Entry<URI, IMethod1<IWebSocketClientContext>>>()
                {
                    @Override
                    public Boolean invoke(
                            Entry<URI, IMethod1<IWebSocketClientContext>> x)
                            throws Exception
                    {
                        return x.getKey().getPath().equals(address.getPath());
                    }
                });
        
        return isAny;
    }
    
    
    private InetSocketAddress myAddress;
    private TcpListenerProvider myTcpListener;
    
    private HashSet<Entry<URI, IMethod1<IWebSocketClientContext>>> myHandlers = new HashSet<Entry<URI, IMethod1<IWebSocketClientContext>>>();
    
    private IMethod1<Socket> myHandleConnectionHandler = new IMethod1<Socket>()
    {
        @Override
        public void invoke(Socket t) throws Exception
        {
            handleConnection(t);
        }
    };
    
    private String TracedObject()
    {
        return "WebSocketHostListener ";
    }
}
