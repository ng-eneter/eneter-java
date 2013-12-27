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

class DefaultInputConnector implements IInputConnector
{
    private class DefaultResponseSender implements ISender
    {
        public DefaultResponseSender(String clientConnectorAddress, IMessagingProvider messagingProvider)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectorAddress = clientConnectorAddress;
                myMessagingProvider = messagingProvider;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
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
                myMessagingProvider.sendMessage(myClientConnectorAddress, message);
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
        
        private String myClientConnectorAddress;
        private IMessagingProvider myMessagingProvider;
    }

    
    
    public DefaultInputConnector(String serviceConnectorAddress, IMessagingProvider messagingProvider)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceConnectorAddress = serviceConnectorAddress;
            myMessagingProvider = messagingProvider;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void startListening(final IFunction1<Boolean, MessageContext> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (messageHandler == null)
                {
                    throw new IllegalArgumentException("Input parameter messageHandler is null.");
                }
                
                myMessagingProvider.registerMessageHandler(myServiceConnectorAddress, new IMethod1<Object>()
                {
                    @Override
                    public void invoke(Object x) throws Exception
                    {
                        messageHandler.invoke(new MessageContext(x, "", null));
                    }
                });
                myIsListeningFlag = true;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                myIsListeningFlag = false;
                myMessagingProvider.unregisterMessageHandler(myServiceConnectorAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            return myIsListeningFlag;
        }
    }

    @Override
    public ISender createResponseSender(String responseReceiverAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ISender aResponseSender = new DefaultResponseSender(responseReceiverAddress, myMessagingProvider);
            return aResponseSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private String myServiceConnectorAddress;
    private IMessagingProvider myMessagingProvider;
    private Object myListeningManipulatorLock = new Object();
    private boolean myIsListeningFlag;
}
