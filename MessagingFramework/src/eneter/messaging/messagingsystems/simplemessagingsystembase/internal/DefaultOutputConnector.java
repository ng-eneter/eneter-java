/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.OutputStream;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.*;

class DefaultOutputConnector implements IOutputConnector
{

    public DefaultOutputConnector(String serviceConnectorAddress, String clientConnectorAddress, IMessagingProvider messagingProvider)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceConnectorAddress = serviceConnectorAddress;
            myClientConnectorAddress = clientConnectorAddress;
            myMessagingProvider = messagingProvider;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    

    @Override
    public void openConnection(final IFunction1<Boolean, MessageContext> responseMessageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (responseMessageHandler == null)
                {
                    throw new IllegalArgumentException("Input parameter responseMessageHandler is null.");
                }
                
                myMessagingProvider.registerMessageHandler(myClientConnectorAddress, new IMethod1<Object>()
                {
                    @Override
                    public void invoke(Object x) throws Exception
                    {
                        responseMessageHandler.invoke(new MessageContext(x, "", null));
                    }
                });
                myIsResponseListenerRegistered = true;
                myIsConnected = true;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                myIsConnected = false;
                if (myIsResponseListenerRegistered)
                {
                    myMessagingProvider.unregisterMessageHandler(myClientConnectorAddress);
                    myIsResponseListenerRegistered = false;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulatorLock)
        {
            return myIsConnected;
        }
    }
    
    @Override
    public boolean isStreamWritter()
    {
        return false;
    }
    
    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                myMessagingProvider.sendMessage(myServiceConnectorAddress, message);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void sendMessage(IMethod1<OutputStream> toStreamWritter)
            throws Exception
    {
        throw new UnsupportedOperationException("To stream writer is not supported.");
    }
    
    
    private String myServiceConnectorAddress;
    private String myClientConnectorAddress;
    private IMessagingProvider myMessagingProvider;
    private boolean myIsConnected;
    private boolean myIsResponseListenerRegistered;
    private Object myConnectionManipulatorLock = new Object();
}
