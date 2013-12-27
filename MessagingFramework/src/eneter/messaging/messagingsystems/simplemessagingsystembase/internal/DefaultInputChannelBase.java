/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.InputStream;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.net.system.IFunction1;
import eneter.net.system.internal.StringExt;


abstract class DefaultInputChannelBase
{
    public DefaultInputChannelBase(String channelId,
            IInvoker workingThreadInvoker,
            IProtocolFormatter<?> protocolFormatter,
            IInputConnectorFactory serviceConnectorFactory) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (StringExt.isNullOrEmpty(channelId))
                {
                    EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                    throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
                }

                myChannelId = channelId;
                myWorkingThreadInvoker = workingThreadInvoker;
                myProtocolFormatter = protocolFormatter;
                myServiceConnector = serviceConnectorFactory.createInputConnector(channelId);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
    
    public String getChannelId()
    {
        return myChannelId;
    }
    
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                // If the channel is already listening.
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Start the thread model for processing messages.
                    myWorkingThreadInvoker.start();

                    // Start listen to messages.
                    myServiceConnector.startListening(myHandleMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    // The listening did not start correctly.
                    // So try to clean.
                    stopListening();

                    throw err;
                }
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
            synchronized (myListeningManipulatorLock)
            {
                try
                {
                    // Try to close connected clients.
                    disconnectClients();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to disconnect connected clients.", err);
                }

                try
                {
                    myServiceConnector.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }

                try
                {
                    myWorkingThreadInvoker.stop();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to stop ThreadInvoker.", err);
                }
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
            synchronized (myListeningManipulatorLock)
            {
                return myServiceConnector.isListening();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    protected abstract boolean handleMessage(MessageContext messageContext) throws Exception;
    
    // Utility method used by derived input channels to decode the incoming message
    // into the ProtocolMessage.
    protected ProtocolMessage getProtocolMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = null;

            if (message instanceof InputStream)
            {
                aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)message);
            }
            else
            {
                aProtocolMessage = myProtocolFormatter.decodeMessage(message);
            }

            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    protected abstract void disconnectClients();
    
    protected IInputConnector myServiceConnector;
    protected IProtocolFormatter<?> myProtocolFormatter;
    protected IInvoker myWorkingThreadInvoker;
    
    protected String myChannelId;

    private Object myListeningManipulatorLock = new Object();
    
    
    IFunction1<Boolean, MessageContext> myHandleMessage = new IFunction1<Boolean, MessageContext>()
    {
        @Override
        public Boolean invoke(MessageContext x) throws Exception
        {
            return handleMessage(x);
        }
    };
    
    protected String TracedObject()
    {
        return getClass().getSimpleName()+ " '" + myChannelId + "' ";
    }
}
