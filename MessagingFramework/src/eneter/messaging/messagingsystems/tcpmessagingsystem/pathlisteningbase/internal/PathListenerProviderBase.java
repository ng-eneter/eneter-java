/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal;

import java.net.URI;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.IMethod1;

public abstract class PathListenerProviderBase
{

    public PathListenerProviderBase(IHostListenerFactory hostListenerFactory, URI webSocketUri)
    {
        this(hostListenerFactory, webSocketUri, new NoneSecurityServerFactory());
    }
    
    public PathListenerProviderBase(IHostListenerFactory hostListenerFactory, URI webSocketUri, IServerSecurityFactory securityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myHostListenerFactory = hostListenerFactory;
            myAddress = webSocketUri;
            mySecurityFactory = securityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void startListening(IMethod1<Object> connectionHandler) throws Exception
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

                    HostListenerController.startListening(myAddress, myHostListenerFactory, myConnectionHandler, mySecurityFactory);
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
    
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myListeningManipulatorLock)
                {
                    HostListenerController.stopListening(myAddress);
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
    
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return HostListenerController.isListening(myAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public URI getAddress()
    {
        return myAddress;
    }
    
    
    private URI myAddress;
    private IHostListenerFactory myHostListenerFactory;
    private IMethod1<Object> myConnectionHandler;
    private IServerSecurityFactory mySecurityFactory;
    private Object myListeningManipulatorLock = new Object();
    
    protected abstract String TracedObject();
}
