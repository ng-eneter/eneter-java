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
import java.util.HashMap;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.IMethod1;
import eneter.net.system.internal.*;


class UdpInputConnector implements IInputConnector
{
    private class TClientContext
    {
        public TClientContext(DatagramSocket udpSocket, InetSocketAddress clientAddress)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myUdpSocket = udpSocket;
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
                DatagramPacket aPacket = new DatagramPacket(aMessageData, aMessageData.length, myClientAddress);
                myUdpSocket.send(aPacket);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private DatagramSocket myUdpSocket;
        private InetSocketAddress myClientAddress;
    }
    
    public UdpInputConnector(String ipAddressAndPort, IProtocolFormatter protocolFormatter) throws Exception
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
                myReceiver = new UdpReceiver(myServiceEndpoint, true);
                myReceiver.startListening(myOnRequestMessageReceived);
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
                Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(outputConnectorAddress);
                aClientContext.sendResponseMessage(anEncodedMessage);
                aClientContext.closeConnection();
            }
            else
            {
                throw new IllegalStateException("Could not send close connection message because the response receiver '" + outputConnectorAddress + "' was not found.");
            }
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
            String aClientIp = (clientAddress != null) ? clientAddress.getAddress().toString() : "";

            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(datagram);

            if (aProtocolMessage != null)
            {
                MessageContext aMessageContext = null;

                if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    if (!StringExt.isNullOrEmpty(aProtocolMessage.ResponseReceiverId))
                    {
                        myConnectedClientsLock.lock();
                        try
                        {
                            if (!myConnectedClients.containsKey(aProtocolMessage.ResponseReceiverId))
                            {
                                TClientContext aClientContext = new TClientContext(myReceiver.getUdpSocket(), clientAddress);
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

                try
                {
                    aMessageContext = new MessageContext(aProtocolMessage, aClientIp);
                    myMessageHandler.invoke(aMessageContext);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private IProtocolFormatter myProtocolFormatter;
    private InetSocketAddress myServiceEndpoint;
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
