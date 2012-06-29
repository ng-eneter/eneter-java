/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.*;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.linq.EnumerableExt;

class WebSocketListenerController
{

    public static void startListening(URI address, IMethod1<IWebSocketClientContext> processConnection, IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeners)
                {
                    // Get TCP listener.
                    WebSocketHostListener aHostListener = findHostListener(address);
                    if (aHostListener == null)
                    {
                        // Listener does not exist yet, so create new one.
                        InetSocketAddress anAddress = new InetSocketAddress(address.getHost(), address.getPort());
                        aHostListener = new WebSocketHostListener(anAddress, serverSecurityFactory);

                        myListeners.add(aHostListener);
                    }

                    // Register the path listener.
                    aHostListener.registerListener(address, processConnection);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to start listening to web sockets.", err);
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
                    WebSocketHostListener aHostListener = findHostListener(uri);
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
                EneterTrace.warning(TracedObject() + "failed to stop listening to web sockets.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static boolean isListening(URI uri) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeners)
            {
                // Get host listener.
                WebSocketHostListener aHostListener = findHostListener(uri);
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
    
    private static WebSocketHostListener findHostListener(URI uri)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get host listener.
            final InetSocketAddress anAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
            WebSocketHostListener aHostListener = EnumerableExt.firstOrDefault(myListeners, new IFunction1<Boolean, WebSocketHostListener>()
            {
                @Override
                public Boolean invoke(WebSocketHostListener x) throws Exception
                {
                    return x.getAddress().equals(anAddress);
                }
            });
                    
            return aHostListener;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private static ArrayList<WebSocketHostListener> myListeners = new ArrayList<WebSocketHostListener>();
    
    private static String TracedObject()
    {
        return "WebSocketListenerController ";
    }
}
