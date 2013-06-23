/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.ISender;

import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.internal.IDisposable;

class WebSocketServiceConnector implements IServiceConnector
{
    private class WebSocketResponseSender implements ISender, IDisposable
    {
        public WebSocketResponseSender(IWebSocketClientContext clientContext)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientContext = clientContext;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public void dispose()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientContext.closeConnection();
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
                myClientContext.sendMessage(message);
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
            throw new UnsupportedOperationException();
        }
        
        
        private IWebSocketClientContext myClientContext;
    }

    
    
    public WebSocketServiceConnector(String wsUriAddress, IServerSecurityFactory securityFactory) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI aUri;
            try
            {
                aUri = new URI(wsUriAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(wsUriAddress + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myListener = new WebSocketListener(aUri, securityFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void startListening(
            IFunction1<Boolean, MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListenerManipulatorLock)
            {
                try
                {
                    myMessageHandler = messageHandler;
                    myListener.startListening(myHandleConnection);
                }
                catch (Exception err)
                {
                    myMessageHandler = null;
                    throw err;
                }
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
            synchronized (myListenerManipulatorLock)
            {
                myListener.stopListening();
                myMessageHandler = null;
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
        synchronized (myListenerManipulatorLock)
        {
            try
            {
                return myListener.isListening();
            }
            catch (Exception err)
            {
                EneterTrace.error(ErrorHandler.DetectedException, err);
            }
            
            return false;
        }
    }

    @Override
    public ISender createResponseSender(String responseReceiverAddress)
    {
        throw new UnsupportedOperationException("CreateResponseSender is not supported in WebSocketServiceConnector.");
    }

    
    private void handleConnection(IWebSocketClientContext client) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                String aClientIp = (client.getClientEndPoint() != null) ? client.getClientEndPoint().getAddress().toString() : "";
                ISender aResponseSender = new WebSocketResponseSender(client);

                boolean isConnectionOpen = true;
                while (isConnectionOpen)
                {
                    // Block until a message is received or the connection is closed.
                    WebSocketMessage aWebSocketMessage = client.receiveMessage();

                    if (aWebSocketMessage != null && myMessageHandler != null)
                    {
                        MessageContext aContext = new MessageContext(aWebSocketMessage.getInputStream(), aClientIp, aResponseSender);
                        if (!myMessageHandler.invoke(aContext))
                        {
                            isConnectionOpen = false;
                        }

                        if (!client.isConnected())
                        {
                            isConnectionOpen = false;
                        }
                    }
                    else
                    {
                        isConnectionOpen = false;
                    }
                }
            }
            finally
            {
                client.closeConnection();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IMethod1<IWebSocketClientContext> myHandleConnection = new IMethod1<IWebSocketClientContext>()
    {
        @Override
        public void invoke(IWebSocketClientContext t) throws Exception
        {
            handleConnection(t);
        }
    };
    
    private WebSocketListener myListener;
    private IFunction1<Boolean, MessageContext> myMessageHandler;
    private Object myListenerManipulatorLock = new Object();
    private ArrayList<IWebSocketClientContext> myConnectedSenders = new ArrayList<IWebSocketClientContext>();
}
