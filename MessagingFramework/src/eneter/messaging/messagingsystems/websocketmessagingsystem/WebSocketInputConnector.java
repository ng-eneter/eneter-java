/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.MessageContext;

import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.internal.*;

class WebSocketInputConnector implements IInputConnector
{
    private class TClientContext
    {
        public TClientContext(IWebSocketClientContext client)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClient = client;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void closeConnection()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myIsClosedFromService = true;
                myClient.closeConnection();
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void SendResponseMessage(Object message) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClient.sendMessage(message);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public boolean isClosedFromService()
        {
            return myIsClosedFromService;
        }
        
        private IWebSocketClientContext myClient;
        private boolean myIsClosedFromService;
    }

    
    
    public WebSocketInputConnector(String wsUriAddress, IProtocolFormatter protocolFormatter, IServerSecurityFactory securityFactory) throws Exception
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

            myProtocolFormatter = protocolFormatter;
            myListener = new WebSocketListener(aUri, securityFactory);
            mySecurityFactory = securityFactory;
            
            // Check if protocol encodes open and close messages.
            myProtocolUsesOpenConnectionMessage = myProtocolFormatter.encodeOpenConnectionMessage("test") != null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void startListening(IMethod1<MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageHandler == null)
            {
                throw new IllegalArgumentException("messageHandler is null.");
            }
            
            synchronized (myListenerManipulatorLock)
            {
                try
                {
                    myMessageHandler = messageHandler;
                    myListener.startListening(myHandleConnection);
                }
                catch (Exception err)
                {
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
    public void sendResponseMessage(String outputConnectorAddress, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TClientContext aClientContext;
            synchronized (myConnectedClients)
            {
                aClientContext = myConnectedClients.get(outputConnectorAddress);
            }

            if (aClientContext == null)
            {
                throw new IllegalStateException("The connection with client '" + outputConnectorAddress + "' is not open.");
            }

            Object anEncodedMessage = myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
            aClientContext.SendResponseMessage(anEncodedMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void closeConnection(String outputConnectorAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TClientContext aClientContext;
            synchronized (myConnectedClients)
            {
                aClientContext = myConnectedClients.get(outputConnectorAddress);
            }

            if (aClientContext != null)
            {
                aClientContext.closeConnection();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleConnection(IWebSocketClientContext client) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aClientIp = (client.getClientEndPoint() != null) ? client.getClientEndPoint().getAddress().toString() : "";
            
            TClientContext aClientContext = new TClientContext(client);
            String aClientId = null;
            try
            {
                client.setSendTimeout(mySecurityFactory.getSendTimeout());
                client.setReceiveTimeout(mySecurityFactory.getReceiveTimeout());
                
                // If protocol formatter does not use OpenConnection message.
                if (!myProtocolUsesOpenConnectionMessage)
                {
                    aClientId = UUID.randomUUID().toString();
                    synchronized (myConnectedClients)
                    {
                        myConnectedClients.put(aClientId, aClientContext);
                    }

                    ProtocolMessage anOpenConnectionProtocolMessage = new ProtocolMessage(EProtocolMessageType.OpenConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(anOpenConnectionProtocolMessage, aClientIp);
                    myMessageHandler.invoke(aMessageContext);
                }
                
                while (true)
                {
                    // Block until a message is received or the connection is closed.
                    WebSocketMessage aWebSocketMessage = client.receiveMessage();

                    if (aWebSocketMessage != null && myMessageHandler != null)
                    {
                        ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)aWebSocketMessage.getInputStream());

                        // Note: security reasons ignore close connection message in WebSockets.
                        //       So that it is not possible that somebody will just send a close message which will have id of somebody else.
                        //       The connection will be closed when the client closes the socket.
                        if (aProtocolMessage != null && aProtocolMessage.MessageType != EProtocolMessageType.CloseConnectionRequest)
                        {
                            MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientIp);

                            // If protocol formatter uses open connection message to create the connection.
                            if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest &&
                                myProtocolUsesOpenConnectionMessage)
                            {
                                // Note: if client id is already set then it means this client has already open connection.
                                if (StringExt.isNullOrEmpty(aClientId))
                                {
                                    aClientId = !StringExt.isNullOrEmpty(aProtocolMessage.ResponseReceiverId) ? aProtocolMessage.ResponseReceiverId : UUID.randomUUID().toString();

                                    synchronized (myConnectedClients)
                                    {
                                        if (!myConnectedClients.containsKey(aClientId))
                                        {
                                            myConnectedClients.put(aClientId, aClientContext);
                                        }
                                        else
                                        {
                                            // Note: if the client id already exists then the connection cannot be open
                                            //       and the connection with this  client will be closed.
                                            EneterTrace.warning(TracedObject() + "could not open connection for client '" + aClientId + "' because the client with same id is already connected.");
                                            break;
                                        }
                                    }
                                }
                                else
                                {
                                    EneterTrace.warning(TracedObject() + "the client '" + aClientId + "' has already open connection.");
                                }
                            }

                            // Notify message.
                            try
                            {
                                // Ensure that nobody will try to use id of somebody else.
                                aMessageContext.getProtocolMessage().ResponseReceiverId = aClientId;

                                myMessageHandler.invoke(aMessageContext);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                        else if (aProtocolMessage == null)
                        {
                            // Client disconnected. Or the client shall be disconnected because of incorrect message format.
                            break;
                        }
                    }
                    else
                    {
                        break;
                    }
                }
            }
            finally
            {
                // Remove client from connected clients.
                if (aClientId != null)
                {
                    synchronized (myConnectedClients)
                    {
                        myConnectedClients.remove(aClientId);
                    }
                }

                // If the disconnection does not come from the service
                // and the client was successfuly connected then notify about the disconnection.
                if (!aClientContext.isClosedFromService() && aClientId != null)
                {
                    ProtocolMessage aCloseProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(aCloseProtocolMessage, aClientIp);

                    // Notify duplex input channel about the disconnection.
                    try
                    {
                        myMessageHandler.invoke(aMessageContext);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }

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
    
    private IProtocolFormatter myProtocolFormatter;
    private boolean myProtocolUsesOpenConnectionMessage;
    
    private WebSocketListener myListener;
    private IMethod1<MessageContext> myMessageHandler;
    private Object myListenerManipulatorLock = new Object();
    private HashMap<String, TClientContext> myConnectedClients = new HashMap<String, WebSocketInputConnector.TClientContext>();
    private IServerSecurityFactory mySecurityFactory;
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
