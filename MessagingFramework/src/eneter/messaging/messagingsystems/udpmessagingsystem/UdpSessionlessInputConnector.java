/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */


package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.IMethod1;
import eneter.net.system.internal.*;


class UdpSessionlessInputConnector implements IInputConnector
{
    public UdpSessionlessInputConnector(String ipAddressAndPort, IProtocolFormatter protocolFormatter,
            boolean reuseAddressFlag, int ttl, boolean allowBroadcast, String multicastGroup, boolean multicastLoopbackFlag)
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

            URI anServiceUri;
            try
            {
                // just check if the address is valid
                anServiceUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(ipAddressAndPort + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            int aPort = (anServiceUri.getPort() < 0) ? 0 : anServiceUri.getPort();
            myServiceEndpoint = new InetSocketAddress(anServiceUri.getHost(), aPort);
            myProtocolFormatter = protocolFormatter;
            myReuseAddressFlag = reuseAddressFlag;
            myAllowBroadcastFlag = allowBroadcast;
            myTtl = ttl;
            if (!StringExt.isNullOrEmpty(multicastGroup))
            {
                myMulticastGroup = IPAddressExt.parseMulticastGroup(multicastGroup);
            }
            myMulticastLoopbackFlag = multicastLoopbackFlag;
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
                    myReceiver = UdpReceiver.createBoundReceiver(myServiceEndpoint, myReuseAddressFlag, myTtl, myAllowBroadcastFlag, myMulticastGroup, myMulticastLoopbackFlag);
                    myReceiver.startListening(myOnRequestMessageReceived);
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
            URI aUri = new URI(outputConnectorAddress);
            InetSocketAddress anEndpoint = new InetSocketAddress(aUri.getHost(), aUri.getPort());
            byte[] anEncodedMessage = (byte[]) myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
            myReceiver.sendTo(anEncodedMessage, anEndpoint);
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
            throw new UnsupportedOperationException("Instead use SendResponseMessage(\"udp://255.255.255.255:xxxx\", message);");
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
                aProtocolMessage.ResponseReceiverId = "udp://" + aClientIp + "/";
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientIp);

                try
                {
                    IMethod1<MessageContext> aMessageHandler = myMessageHandler;
                    if (aMessageHandler != null)
                    {
                        aMessageHandler.invoke(aMessageContext);
                    }
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
    private InetAddress myMulticastGroup;
    private boolean myReuseAddressFlag;
    private int myTtl;
    private boolean myAllowBroadcastFlag;
    private boolean myMulticastLoopbackFlag;
    private UdpReceiver myReceiver;
    private ThreadLock myListenerManipulatorLock = new ThreadLock();
    private IMethod1<MessageContext> myMessageHandler;
    
    private IMethod2<byte[], InetSocketAddress> myOnRequestMessageReceived = new IMethod2<byte[], InetSocketAddress>()
    {
        @Override
        public void invoke(byte[] x, InetSocketAddress y) throws Exception
        {
            onRequestMessageReceived(x, y);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
