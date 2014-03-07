/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.IFunction1;
import eneter.net.system.IMethod1;
import eneter.net.system.linq.internal.EnumerableExt;


/**
 * Internal static class maintaining registered hostlisteners
 * listening to particular protocol and registered paths.
 *
 */
class HostListenerController
{
    /**
     * Starts listening to the given URI.
     * @param address path to be listened.
     * @param hostListenerFactory The factory creating the host listener if does not exist.
     * @param connectionHandler Handler processing the connection.
     * @param serverSecurityFactory The factory providing the security functionality on the server side.
     * @throws Exception
     */
    public static void startListening(URI address, 
                                      IHostListenerFactory hostListenerFactory,
                                      IMethod1<Object> connectionHandler,
                                      IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeners)
                {
                    // Get listener for the address specified in the given uri.
                    HostListenerBase aHostListener = getHostListener(address);
                    if (aHostListener == null)
                    {
                        // Listener does not exist yet, so create new one.
                        InetSocketAddress anAddress = new InetSocketAddress(address.getHost(), address.getPort());
                        aHostListener = hostListenerFactory.CreateHostListener(anAddress, serverSecurityFactory);

                        // Register the path listener.
                        aHostListener.registerListener(address, connectionHandler);
                        
                        myListeners.add(aHostListener);
                    }
                    else
                    {
                        // If found listener is listening to another protocol.
                        // e.g. if I want to start listening to http but websocket listener is listening on
                        //      the given IP address and port.
                        if (aHostListener.getClass() != hostListenerFactory.getListenerType())
                        {
                            String anErrorMessage = TracedObject + "failed to start " + hostListenerFactory.getListenerType().toString() + " because " + aHostListener.getClass().toString() + " is already listening on IP address and port.";
                            EneterTrace.error(anErrorMessage);
                            throw new IllegalStateException(anErrorMessage);
                        }
                        
                        // Register the path listener.
                        aHostListener.registerListener(address, connectionHandler);
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject + "failed to start listening to web sockets.", err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static void stopListening(URI uri)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeners)
                {
                    // Get host listener.
                    HostListenerBase aHostListener = getHostListener(uri);
                    if (aHostListener == null)
                    {
                        return;
                    }

                    // Unregister the path listener.
                    aHostListener.unregisterListener(uri);

                    // If there is no a path listener then nobody is interested in incoming
                    // HTTP requests and the TCP listening can be stopped.
                    if (aHostListener.existAnyListener() == false)
                    {
                        myListeners.remove(aHostListener);
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject + "failed to stop listening to web sockets.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static boolean isListening(URI uri)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeners)
            {
                // Get host listener.
                HostListenerBase aHostListener = getHostListener(uri);
                if (aHostListener == null)
                {
                    return false;
                }

                // If the path listener does not exist then listening is not active.
                if (aHostListener.existListener(uri) == false)
                {
                    return false;
                }

                return true;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private static HostListenerBase getHostListener(URI uri)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get host listener.
            final InetSocketAddress anAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
            HostListenerBase aHostListener = EnumerableExt.firstOrDefault(myListeners, new IFunction1<Boolean, HostListenerBase>()
            {
                @Override
                public Boolean invoke(HostListenerBase x)
                {
                    return x.getAddress().equals(anAddress);
                }
            });
                    
            return aHostListener;
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject + "failed to compare addresses when searching for the host listener.", err);
            return null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    // List of IP address : port listeners. These listeners then maintain particular path listeners.
    private static ArrayList<HostListenerBase> myListeners = new ArrayList<HostListenerBase>();
    
    private static final String TracedObject = "HostListenerController ";
}
