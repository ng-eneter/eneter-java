package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.*;
import eneter.net.system.linq.EnumerableExt;

class HttpListenerController
{
    // Sublistener listening to a particular path.
    private static class PathListener
    {
        public PathListener(String absolutPath, IMethod1<Socket> connectionHandler)
        {
            myAbsolutePath = absolutPath;
            myConnectionHandler = connectionHandler;
        }
        
        public String getPath()
        {
            return myAbsolutePath;
        }
        
        public IMethod1<Socket> getConnectionHandler()
        {
            return myConnectionHandler;
        }
        
        // Path identifying the sub-listener.
        private String myAbsolutePath;
        
        // Handler provided by a user to process the connection.
        private IMethod1<Socket> myConnectionHandler;
    }
    
    // Listener listening to a particular IP address and port.
    // One HostListener can have more PathListeners.
    private static class HostListener
    {
        public HostListener(InetSocketAddress socketAddress, IServerSecurityFactory serverSecurityFactory)
        {
            myTcpListener = new TcpListenerProvider(socketAddress, serverSecurityFactory);
        }
        
        public InetSocketAddress getSocketAddress()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return myTcpListener.getSocketAddress();
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public void registerPathListener(final String path, IMethod1<Socket> connectionHandler)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (myPathListeners)
                {
                 // If the path listener already exists then error, because only one instance can listen.
                    if (EnumerableExt.any(myPathListeners,
                            new IFunction1<Boolean, PathListener>()
                            {
                                @Override
                                public Boolean invoke(PathListener x)
                                        throws Exception
                                {
                                    return x.getPath().equals(path);
                                }
                            }))
                    {
                        // The listener already exists.
                        String anErrorMessage = TracedObject() + "detected the address is already used.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                    
                    // Create path listener.
                    PathListener aPathListener = new PathListener(path, connectionHandler);
                    
                    // Add path-listener to the host-listener - particular path listeners listen to different paths.
                    myPathListeners.add(aPathListener);
                    
                    // If the host listen does not listen to sockets yet, then start it.
                    if (myTcpListener.isListening() == false)
                    {
                        try
                        {
                            myTcpListener.startListening(myConnectionHandler);
                        }
                        catch (Exception err)
                        {
                            unregisterPathListener(path);
                        }
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public void unregisterPathListener(final String path)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (myPathListeners)
                {
                    // Get path listener.
                    PathListener aPathListener = EnumerableExt.firstOrDefault(myPathListeners,
                            new IFunction1<Boolean, PathListener>()
                            {
                                @Override
                                public Boolean invoke(PathListener x)
                                        throws Exception
                                {
                                    return x.getPath().equals(path);
                                }
                            });
                    if (aPathListener == null)
                    {
                        // Nothing to do.
                        return;
                    }
                    
                    // Remove the path-listener from the host-listener.
                    myPathListeners.remove(aPathListener);
                    
                    // If there is no path-listener, then stop the listening.
                    if (myPathListeners.size() == 0)
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
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        

        // Handles TCP connection.
        // It parses the HTTP request to get the requested path.
        // Then it searches the matching PathListeners and calls it to handle the connection.
        private void handleConnection(Socket clientSocket) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                // Read the path from the HTTP part.
                String aHostAndPath = "";
                int aWordIdx = 0;
                InputStream anInputStream = clientSocket.getInputStream();
                DataInputStream aReader = new DataInputStream(anInputStream);
                while (true)
                {
                    int aValue = aReader.read();
                    
                    // If a space character, then the next word start.
                    if (aValue == 32)
                    {
                        ++aWordIdx;
                    }
                    else if (aValue != -1 && aWordIdx == 1)
                    {
                        // The second word is the path, so store it.
                        aHostAndPath += Character.toString((char)aValue);
                    }
                    
                    // End of some line
                    if (aValue == 13)
                    {
                        aValue = aReader.read();
                        if (aValue == 10)
                        {
                            // Follows empty line.
                            aValue = aReader.read();
                            if (aValue == 13)
                            {
                                aValue = aReader.read();
                                if (aValue == 10)
                                {
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (aValue == -1)
                    {
                        String anErrorMessage = TracedObject() + "detected unexpected end of the input stream.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                }
                
                PathListener aPathListener;
                synchronized (myPathListeners)
                {
                    // Get the path listener.
                    final URI aUri = new URI(aHostAndPath);
                    aPathListener = EnumerableExt.firstOrDefault(myPathListeners,
                            new IFunction1<Boolean, PathListener>()
                            {
                                @Override
                                public Boolean invoke(PathListener x)
                                        throws Exception
                                {
                                    return x.equals(aUri.getPath());
                                }
                            });
                }
                
                // if alistener exist, then invoke it to process the request.
                if (aPathListener != null)
                {
                    aPathListener.getConnectionHandler().invoke(clientSocket);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private TcpListenerProvider myTcpListener;
        private ArrayList<PathListener> myPathListeners = new ArrayList<PathListener>();
        
        private IMethod1<Socket> myConnectionHandler = new IMethod1<Socket>()
        {
            @Override
            public void invoke(Socket x) throws Exception
            {
                handleConnection(x);
            }
        };
    }
    
    public static void startListening(final URI uri, IMethod1<Socket> connectionHandler, IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeners)
            {
                final InetSocketAddress aSocketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
                
                // Get host listener.
                HostListener aHostListener = EnumerableExt.firstOrDefault(myListeners,
                        new IFunction1<Boolean, HostListener>()
                        {
                            @Override
                            public Boolean invoke(HostListener x) throws Exception
                            {
                                return x.getSocketAddress().equals(aSocketAddress);
                            }
                        });
                if (aHostListener == null)
                {
                    aHostListener = new HostListener(aSocketAddress, serverSecurityFactory);
                }
                
                aHostListener.registerPathListener(uri.getPath(), connectionHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public static void stopListening(final URI aUri)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeners)
            {
                final InetSocketAddress aSocketAddress = new InetSocketAddress(aUri.getHost(), aUri.getPort());
                
                // Get host listener.
                HostListener aHostListener = EnumerableExt.firstOrDefault(myListeners,
                        new IFunction1<Boolean, HostListener>()
                        {
                            @Override
                            public Boolean invoke(HostListener x) throws Exception
                            {
                                return x.getSocketAddress().equals(aSocketAddress);
                            }
                        });
                if (aHostListener == null)
                {
                    // If the listener does not exist, then nothing to do.
                    return;
                }
                
                // Get path listener.
                PathListener aPathListener = EnumerableExt.firstOrDefault(aHostListener.getPathListeners(),
                        new IFunction1<Boolean, PathListener>()
                        {
                            @Override
                            public Boolean invoke(PathListener x)
                                    throws Exception
                            {
                                return x.getPath().equals(aUri.getPath());
                            }
                        });
                if (aPathListener == null)
                {
                    // Nothing to do.
                    return;
                }
                
                // Remove the path-listener from the host-listener.
                aHostListener.getPathListeners().remove(aPathListener);
                
                // if it was the last path listener, then stop the host listener too.
                if (aHostListener.getPathListeners().size() == 0)
                {
                    aHostListener.stopListening();
                    
                    // Remove the host listener.
                    myListeners.remove(aHostListener);
                }
                
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static boolean isListening(final URI aUri)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeners)
            {
                final InetSocketAddress aSocketAddress = new InetSocketAddress(aUri.getHost(), aUri.getPort());
                
                // Get host listener.
                HostListener aHostListener = EnumerableExt.firstOrDefault(myListeners,
                        new IFunction1<Boolean, HostListener>()
                        {
                            @Override
                            public Boolean invoke(HostListener x) throws Exception
                            {
                                return x.getSocketAddress().equals(aSocketAddress);
                            }
                        });
                if (aHostListener == null)
                {
                    return false;
                }
                
                if (EnumerableExt.any(aHostListener.getPathListeners(),
                        new IFunction1<Boolean, PathListener>()
                        {
                            @Override
                            public Boolean invoke(PathListener x)
                                    throws Exception
                            {
                                return x.getPath().equals(aUri.getPath());
                            }
                        }))
                {
                    return aHostListener.isListening();
                }
                
                return false;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private static ArrayList<HostListener> myListeners = new ArrayList<HostListener>();
    
    private static String TracedObject()
    {
        return "HttpListenerController ";
    }
}
