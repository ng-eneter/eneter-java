/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;


class TcpInputConnector implements IInputConnector
{
    private class TClientContext
    {
        public TClientContext(OutputStream clientStream, int sendTimeout)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientStream = clientStream;
                mySendTimeout = sendTimeout;
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
                myIsClosedByService = true;
                myClientStream.close();
            }
            catch (IOException err)
            {
                EneterTrace.warning(getClass().getSimpleName() + " failed to close the client socket.", err);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void sendResponseMessage(Object message) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                mySenderLock.lock();
                try
                {
                    byte[] aMessage = (byte[])message;
                    myStreamWriter.write(myClientStream, aMessage, mySendTimeout);
                }
                finally
                {
                    mySenderLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public boolean isClosedByService()
        {
            return myIsClosedByService;
        }
        
        private OutputStream myClientStream;
        private int mySendTimeout;
        private ThreadLock mySenderLock = new ThreadLock();
        private OutputStreamTimeoutWriter myStreamWriter = new OutputStreamTimeoutWriter();
        private boolean myIsClosedByService;
    }

    
    
    public TcpInputConnector(String ipAddressAndPort, IProtocolFormatter protocolFormatter, IServerSecurityFactory securityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTcpListenerProvider = new TcpListenerProvider(ipAddressAndPort, securityFactory);
            myProtocolFormatter = protocolFormatter;
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
            
            myListeningManipulatorLock.lock();
            try
            {
                try
                {
                    myMessageHandler = messageHandler;
                    myTcpListenerProvider.startListening(myHandleConnection);
                }
                catch (Exception err)
                {
                    stopListening();
                    throw err;
                }
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

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectedClientsLock.lock();
            try
            {
                for (Entry<String, TClientContext> aClientContext : myConnectedClients.entrySet())
                {
                    try
                    {
                        aClientContext.getValue().closeConnection();
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
            
            myListeningManipulatorLock.lock();
            try
            {
                myTcpListenerProvider.stopListening();
                myMessageHandler = null;
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

    @Override
    public boolean isListening()
    {
        myListeningManipulatorLock.lock();
        try
        {
            return myTcpListenerProvider.isListening();
        }
        finally
        {
            myListeningManipulatorLock.unlock();
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
                aClientContext.sendResponseMessage(anEncodedMessage);
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
                        // Send the response message.
                        Object anEncodedMessage = myProtocolFormatter.encodeMessage(aClientContext.getKey(), message);
                        aClientContext.getValue().sendResponseMessage(anEncodedMessage);
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
    
    
    private void handleConnection(Socket clientSocket) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aClientIp = IpAddressUtil.getRemoteIpAddress(clientSocket);

            InputStream anInputStream = clientSocket.getInputStream();
            OutputStream anOutputStream = clientSocket.getOutputStream();
            
            TClientContext aClientContext = null;
            String aClientId = null;
            
            try
            {
                int aSendTimeout = mySecurityFactory.getSendTimeout();
                aClientContext = new TClientContext(anOutputStream, aSendTimeout);
                
                // If current protocol formatter does not support OpenConnection message
                // then open the connection now.
                if (!myProtocolUsesOpenConnectionMessage)
                {
                    // Generate client id.
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
                
                // While the stop of listening is not requested and the connection is not closed.
                while (true)
                {
                    ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)anInputStream);

                    // If the stream was not closed.
                    if (aProtocolMessage != null)
                    {
                        // Note: Due to security reasons ignore close connection message in TCP.
                        //       So that it is not possible that somebody will just send a close message which will have id of somebody else.
                        //       The TCP connection will be closed when the client closes the socket.
                        if (aProtocolMessage.MessageType != EProtocolMessageType.CloseConnectionRequest)
                        {
                            MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientIp);
    
                            // If open connection message is received and the current protocol formatter uses open connection message
                            // then create the connection now.
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
    
                            // Ensure that nobody will try to use id of somebody else.
                            aMessageContext.getProtocolMessage().ResponseReceiverId = aClientId;
                            notifyMessageContext(aMessageContext);
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

                // If the disconnection comes from the client (and not from the service).
                if (aClientContext != null && !aClientContext.isClosedByService())
                {
                    ProtocolMessage aCloseProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(aCloseProtocolMessage, aClientIp);

                    // Notify duplex input channel about the disconnection.
                    notifyMessageContext(aMessageContext);
                }

                if (anInputStream != null)
                {
                    anInputStream.close();
                }
                
                if (anOutputStream != null)
                {
                    anOutputStream.close();
                }
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
    
    
    private TcpListenerProvider myTcpListenerProvider;
    private IServerSecurityFactory mySecurityFactory;
    
    private IProtocolFormatter myProtocolFormatter;
    private boolean myProtocolUsesOpenConnectionMessage;
    private IMethod1<MessageContext> myMessageHandler;
    
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    private ThreadLock myConnectedClientsLock = new ThreadLock();
    private HashMap<String, TClientContext> myConnectedClients = new HashMap<String, TClientContext>();
    
    private IMethod1<Socket> myHandleConnection = new IMethod1<Socket>()
    {
        @Override
        public void invoke(Socket t) throws Exception
        {
            handleConnection(t);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
