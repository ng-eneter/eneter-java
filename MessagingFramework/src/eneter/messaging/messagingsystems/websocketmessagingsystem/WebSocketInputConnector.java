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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
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
            
            myListenerManipulatorLock.lock();
            try
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
            finally
            {
                myListenerManipulatorLock.unlock();
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
            myConnectedClientsLock.lock();
            try
            {
                for (Entry<String, TClientContext> aClient : myConnectedClients.entrySet())
                {
                    try
                    {
                        aClient.getValue().closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
                    }
                }

                myConnectedClients.clear();
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }
            
            myListenerManipulatorLock.lock();
            try
            {
                myListener.stopListening();
                myMessageHandler = null;
            }
            finally
            {
                myListenerManipulatorLock.unlock();
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
        myListenerManipulatorLock.lock();
        try
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
        finally
        {
            myListenerManipulatorLock.unlock();
        }
    }

    @Override
    public void sendResponseMessage(String outputConnectorAddress, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TClientContext aClientContext;
            myConnectedClientsLock.lock();
            try
            {
                aClientContext = myConnectedClients.get(outputConnectorAddress);
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }

            if (aClientContext == null)
            {
                throw new IllegalStateException("The connection with client '" + outputConnectorAddress + "' is not open.");
            }

            try
            {
                Object anEncodedMessage = myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
                aClientContext.SendResponseMessage(anEncodedMessage);
            }
            catch (Exception err)
            {
                closeConnection(outputConnectorAddress, true);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void sendBroadcast(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> aDisconnectedClients = new ArrayList<String>();

            myConnectedClientsLock.lock();
            try
            {
                // Send the response message to all connected clients.
                for (Entry<String, TClientContext> aClientContext : myConnectedClients.entrySet())
                {
                    try
                    {
                        Object anEncodedMessage = myProtocolFormatter.encodeMessage(aClientContext.getKey(), message);
                        aClientContext.getValue().SendResponseMessage(anEncodedMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);
                        aDisconnectedClients.add(aClientContext.getKey());

                        // Note: Exception is not rethrown because if sending to one client fails it should not
                        //       affect sending to other clients.
                    }
                }
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }

            // Disconnect failed clients.
            for (String anOutputConnectorAddress : aDisconnectedClients)
            {
                closeConnection(anOutputConnectorAddress, true);
            }
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
            closeConnection(outputConnectorAddress, false);
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
            String aClientIp = (client.getClientEndPoint() != null) ? client.getClientEndPoint().toString() : "";
            
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
                    myConnectedClientsLock.lock();
                    try
                    {
                        myConnectedClients.put(aClientId, aClientContext);
                    }
                    finally
                    {
                        myConnectedClientsLock.unlock();
                    }

                    ProtocolMessage anOpenConnectionProtocolMessage = new ProtocolMessage(EProtocolMessageType.OpenConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(anOpenConnectionProtocolMessage, aClientIp);
                    notifyMessageContext(aMessageContext);
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

                                    myConnectedClientsLock.lock();
                                    try
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
                                    finally
                                    {
                                        myConnectedClientsLock.unlock();
                                    }
                                }
                                else
                                {
                                    EneterTrace.warning(TracedObject() + "the client '" + aClientId + "' has already open connection.");
                                }
                            }

                            // Notify message.
                            // Ensure that nobody will try to use id of somebody else.
                            aMessageContext.getProtocolMessage().ResponseReceiverId = aClientId;
                            notifyMessageContext(aMessageContext);
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
                    myConnectedClientsLock.lock();
                    try
                    {
                        myConnectedClients.remove(aClientId);
                    }
                    finally
                    {
                        myConnectedClientsLock.unlock();
                    }
                }

                // If the disconnection does not come from the service
                // and the client was successfuly connected then notify about the disconnection.
                if (!aClientContext.isClosedFromService() && aClientId != null)
                {
                    ProtocolMessage aCloseProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(aCloseProtocolMessage, aClientIp);
                    notifyMessageContext(aMessageContext);
                }

                client.closeConnection();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeConnection(String outputConnectorAddress, boolean notifyFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TClientContext aClientContext;
            myConnectedClientsLock.lock();
            try
            {
                aClientContext = myConnectedClients.get(outputConnectorAddress);
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }

            if (aClientContext != null)
            {
                aClientContext.closeConnection();
            }

            if (notifyFlag)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, outputConnectorAddress, null);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, "");

                notifyMessageContext(aMessageContext);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyMessageContext(MessageContext messageContext)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                IMethod1<MessageContext> aMessageHandler = myMessageHandler;
                if (aMessageHandler != null)
                {
                    aMessageHandler.invoke(messageContext);
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
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
    private ThreadLock myListenerManipulatorLock = new ThreadLock();
    private ThreadLock myConnectedClientsLock = new ThreadLock();
    private HashMap<String, TClientContext> myConnectedClients = new HashMap<String, WebSocketInputConnector.TClientContext>();
    private IServerSecurityFactory mySecurityFactory;
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
