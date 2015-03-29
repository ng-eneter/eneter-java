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


import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
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
                synchronized (mySenderLock)
                {
                    byte[] aMessage = (byte[])message;
                    myStreamWriter.write(myClientStream, aMessage, mySendTimeout);
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
        private Object mySenderLock = new Object();
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
            
            synchronized (myListeningManipulatorLock)
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
                myTcpListenerProvider.stopListening();
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
        synchronized (myListeningManipulatorLock)
        {
            return myTcpListenerProvider.isListening();
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
            aClientContext.sendResponseMessage(anEncodedMessage);
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
                    synchronized (myConnectedClients)
                    {
                        myConnectedClients.put(aClientId, aClientContext);
                    }

                    ProtocolMessage anOpenConnectionProtocolMessage = new ProtocolMessage(EProtocolMessageType.OpenConnectionRequest, aClientId, null);
                    MessageContext aMessageContext = new MessageContext(anOpenConnectionProtocolMessage, aClientIp);

                    try
                    {
                        myMessageHandler.invoke(aMessageContext);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
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
    
                            try
                            {
                                // Ensure that nobody will try to use id of somebody else.
                                aMessageContext.getProtocolMessage().ResponseReceiverId = aClientId;
    
                                // Notify message.
                                myMessageHandler.invoke(aMessageContext);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
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

                // If the disconnection comes from the client (and not from the service).
                if (aClientContext != null && !aClientContext.isClosedByService())
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
    
    private TcpListenerProvider myTcpListenerProvider;
    private IServerSecurityFactory mySecurityFactory;
    
    private IProtocolFormatter myProtocolFormatter;
    private boolean myProtocolUsesOpenConnectionMessage;
    private IMethod1<MessageContext> myMessageHandler;
    
    private Object myListeningManipulatorLock = new Object();
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
