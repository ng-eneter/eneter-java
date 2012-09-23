/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;


abstract class WebSocketInputChannelBase
{
    public WebSocketInputChannelBase(String ipAddressAndPort,
            IInvoker invoker,
            IServerSecurityFactory securityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            myChannelId = ipAddressAndPort;

            URI aUri;
            try
            {
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            myListener = new WebSocketListener(aUri, securityFactory);

            myMessageProcessingWorker = invoker;
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
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Start the working thread processing incoming messages.
                    myMessageProcessingWorker.start();

                    // Start listener.
                    myListener.startListening(myHandleConnectionHandler);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    try
                    {
                        // Clear after failed start
                        stopListening();
                    }
                    catch (Exception err2)
                    {
                        // We tried to clean after failure. The exception can be ignored.
                    }

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
                    // Try to close connections with clients.
                    disconnectClients();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to close websocket connections with clients.", err);
                }

                // Stop the listener.
                myListener.stopListening();

                // Stop thread processing incoming messages.
                myMessageProcessingWorker.stop();
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
                return myListener.isListening();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }

    }
    
    
    protected abstract void disconnectClients();
    
    protected abstract void handleConnection(IWebSocketClientContext client) throws Exception;
    
    
    protected String myChannelId;
    private Object myListeningManipulatorLock = new Object();
    private WebSocketListener myListener;
    protected IInvoker myMessageProcessingWorker;
    
    
    private IMethod1<IWebSocketClientContext> myHandleConnectionHandler = new IMethod1<IWebSocketClientContext>()
    {
        @Override
        public void invoke(IWebSocketClientContext t) throws Exception
        {
            handleConnection(t);
        }
    };
    
    protected abstract String TracedObject();
}
