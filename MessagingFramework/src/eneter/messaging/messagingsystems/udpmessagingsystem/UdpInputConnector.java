/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.IMethod1;
import eneter.net.system.internal.*;


class UdpInputConnector implements IInputConnector
{
    private class TClientContext
    {
        public TClientContext(UdpReceiver udpSocket, InetSocketAddress clientAddress)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myUdpReceiver = udpSocket;
                myClientAddress = clientAddress;
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
                // Note: we do not close the udp socket because it is used globaly for all connected clients.
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void sendResponseMessage(Object message) throws IOException
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                byte[] aMessageData = (byte[])message;
                myUdpReceiver.sendTo(aMessageData, myClientAddress);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private UdpReceiver myUdpReceiver;
        private InetSocketAddress myClientAddress;
    }
    
    public UdpInputConnector(String ipAddressAndPort, IProtocolFormatter protocolFormatter, boolean reuseAddress, int ttl,
            int maxAmountOfConnections)
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

            URI aUri;
            try
            {
                // just check if the address is valid
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(ipAddressAndPort + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myServiceEndpoint = new InetSocketAddress(aUri.getHost(), aUri.getPort());
            myProtocolFormatter = protocolFormatter;
            myReuseAddressFlag = reuseAddress;
            myTtl = ttl;
            myMaxAmountOfConnections = maxAmountOfConnections;
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
                myMessageHandler = messageHandler;
                myReceiver = UdpReceiver.createBoundReceiver(myServiceEndpoint, myReuseAddressFlag, myTtl, false, null, false);
                myReceiver.startListening(myOnRequestMessageReceived);
            }
            catch (Exception err)
            {
                stopListening();
                throw err;
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
                        closeConnection(aClient.getKey(), aClient.getValue());
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
                if (myReceiver != null)
                {
                    myReceiver.stopListening();
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
    public boolean isListening()
    {
        myListenerManipulatorLock.lock();
        try
        {
            return myReceiver != null && myReceiver.isListening();
        }
        finally
        {
            myListenerManipulatorLock.unlock();
        }
    }

    @Override
    public void sendResponseMessage(String outputConnectorAddress,
            Object message) throws Exception
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
    public void closeConnection(String outputConnectorAddress)
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
    
    private void onRequestMessageReceived(byte[] datagram, InetSocketAddress clientAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (datagram == null && clientAddress == null)
            {
                // The listening got interrupted so nothing to do.
                return;
            }

            // Get the sender IP address.
            String aClientIp = (clientAddress != null) ? clientAddress.toString() : "";

            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(datagram);

            if (aProtocolMessage != null)
            {
                if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    if (!StringExt.isNullOrEmpty(aProtocolMessage.ResponseReceiverId))
                    {
                        myConnectedClientsLock.lock();
                        try
                        {
                            if (myMaxAmountOfConnections > -1 && myConnectedClients.size() >= myMaxAmountOfConnections)
                            {
                                TClientContext aClientContext = new TClientContext(myReceiver, clientAddress);
                                closeConnection(aProtocolMessage.ResponseReceiverId, aClientContext);

                                EneterTrace.warning(TracedObject() + "could not open connection for client '" + aProtocolMessage.ResponseReceiverId + "' because the maximum number of connections = '" + myMaxAmountOfConnections + "' was exceeded.");
                                return;
                            }
                            
                            if (!myConnectedClients.containsKey(aProtocolMessage.ResponseReceiverId))
                            {
                                TClientContext aClientContext = new TClientContext(myReceiver, clientAddress);
                                myConnectedClients.put(aProtocolMessage.ResponseReceiverId, aClientContext);
                            }
                            else
                            {
                                EneterTrace.warning(TracedObject() + "could not open connection for client '" + aProtocolMessage.ResponseReceiverId + "' because the client with same id is already connected.");
                            }
                        }
                        finally
                        {
                            myConnectedClientsLock.unlock();
                        }
                    }
                    else
                    {
                        EneterTrace.warning(TracedObject() + "could not connect a client because response recevier id was not available in open connection message.");
                    }
                }
                else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                {
                    if (!StringExt.isNullOrEmpty(aProtocolMessage.ResponseReceiverId))
                    {
                        myConnectedClientsLock.lock();
                        try
                        {
                            myConnectedClients.remove(aProtocolMessage.ResponseReceiverId);
                        }
                        finally
                        {
                            myConnectedClientsLock.unlock();
                        }
                    }
                }

                MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientIp);
                notifyMessageContext(aMessageContext);
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
                myConnectedClients.remove(outputConnectorAddress);
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }

            if (aClientContext != null)
            {
                closeConnection(outputConnectorAddress, aClientContext);
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
    
    private void closeConnection(String outputConnectorAddress, TClientContext clientContext)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(outputConnectorAddress);
                if (anEncodedMessage != null)
                {
                    clientContext.sendResponseMessage(anEncodedMessage);
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
            }
            
            clientContext.closeConnection();
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
    

    private IProtocolFormatter myProtocolFormatter;
    private InetSocketAddress myServiceEndpoint;
    private boolean myReuseAddressFlag;
    private int myTtl;
    private int myMaxAmountOfConnections;
    private UdpReceiver myReceiver;
    private ThreadLock myListenerManipulatorLock = new ThreadLock();
    private IMethod1<MessageContext> myMessageHandler;
    private ThreadLock myConnectedClientsLock = new ThreadLock();
    private HashMap<String, TClientContext> myConnectedClients = new HashMap<String, TClientContext>();
    
    private IMethod2<byte[], InetSocketAddress> myOnRequestMessageReceived = new IMethod2<byte[], InetSocketAddress>()
    {
        @Override
        public void invoke(byte[] datagram, InetSocketAddress clientAddress) throws Exception
        {
            onRequestMessageReceived(datagram, clientAddress);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
