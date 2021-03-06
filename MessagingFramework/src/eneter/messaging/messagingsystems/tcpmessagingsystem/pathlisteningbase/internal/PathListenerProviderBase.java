/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal;

import java.net.URI;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
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
                myListeningManipulatorLock.lock();
                try
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
                finally
                {
                    myListeningManipulatorLock.unlock();
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.FailedToStartListening, err);
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
                myListeningManipulatorLock.lock();
                try
                {
                    HostListenerController.stopListening(myAddress);
                    myConnectionHandler = null;
                }
                finally
                {
                    myListeningManipulatorLock.unlock();
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.IncorrectlyStoppedListening, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myListeningManipulatorLock.lock();
            try
            {
                return HostListenerController.isListening(myAddress);
            }
            finally
            {
                myListeningManipulatorLock.unlock();
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
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    
    protected abstract String TracedObject();
}
