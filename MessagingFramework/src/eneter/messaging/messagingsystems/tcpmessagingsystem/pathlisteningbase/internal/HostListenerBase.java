/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.TcpListenerProvider;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.linq.internal.EnumerableExt;

// for Android
//import eneter.java.util.AbstractMap;



/**
 * Represents the host listener maintaining path listeners for the given protocol.
 * Derived classes implement hostlisteners for protocols.
 * E.g. HttpHostListener, WebSocketHostListener.
 *
 */
public abstract class HostListenerBase
{
    public HostListenerBase(InetSocketAddress address, IServerSecurityFactory securityFactory)
    {
        myAddress = address;
        myTcpListener = new TcpListenerProvider(address, securityFactory);
    }
    
    public InetSocketAddress getAddress()
    {
        return myAddress;
    }
    
    
    /**
     * Starts tcp listening for the IP address and port available from the URI.
     * When the connection is established then it calls abstract method 'handleConnection()'
     * This method is implemented by the particular host listener (e.g. HttpHostListener)
     * and is responsible to read the path from the protocol and forward the incoming
     * messages to the handler provided by the user code.
     * @param address
     * @param processConnection
     * @throws Exception
     */
    public void registerListener(URI address, IMethod1<Object> processConnection)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlers)
            {
                // If the path listener already exists then error, because only one instance can listen.
                if (existListener(address))
                {
                    // The listener already exists.
                    String anErrorMessage = TracedObject() + "detected the address is already used.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }


                // Add handler for this path.
                Entry<URI, IMethod1<Object>> aHandler = new AbstractMap.SimpleEntry<URI, IMethod1<Object>>(address, processConnection);
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
                    HashSetExt.removeWhere(myHandlers, new IFunction1<Boolean, Entry<URI, IMethod1<Object>>>()
                        {
                            @Override
                            public Boolean invoke(Entry<URI, IMethod1<Object>> x) throws Exception
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
    
    public boolean existListener(final URI address)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlers)
            {
                boolean isAny = EnumerableExt.any(myHandlers, new IFunction1<Boolean, Entry<URI, IMethod1<Object>>>()
                    {
                        @Override
                        public Boolean invoke(Entry<URI, IMethod1<Object>> x)
                        {
                            return x.getKey().getPath().equals(address.getPath());
                        }
                    });
                return isAny;
            }
        }
        catch (Exception err)
        {
            EneterTrace.error("Failed to check if the listener exists.", err);
            return false;
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
    
    protected abstract void handleConnection(Socket tcpClient)
            throws Exception;

    private InetSocketAddress myAddress;
    private TcpListenerProvider myTcpListener;

    protected HashSet<Entry<URI, IMethod1<Object>>> myHandlers = new HashSet<Entry<URI, IMethod1<Object>>>();
    
    
    private IMethod1<Socket> myHandleConnectionHandler = new IMethod1<Socket>()
        {
            @Override
            public void invoke(Socket t) throws Exception
            {
                handleConnection(t);
            }
        };
    
    protected abstract String TracedObject();
}
