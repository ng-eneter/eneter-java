/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */


package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IOutputConnector;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.MessageContext;
import eneter.net.system.IMethod1;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.internal.StringExt;

class UdpSessionlessOutputConnector implements IOutputConnector
{
    public UdpSessionlessOutputConnector(String ipAddressAndPort, String outpuConnectorAddress, IProtocolFormatter protocolFormatter,
            boolean reuseAddressFlag, int ttl, boolean allowBroadcast, String multicastGroup, boolean multicastLoopbackFlag)
                    throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI anServiceUri;
            try
            {
                anServiceUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(ipAddressAndPort + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            URI aClientUri;
            try
            {
                aClientUri = new URI(outpuConnectorAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(outpuConnectorAddress + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myServiceEndpoint = new InetSocketAddress(anServiceUri.getHost(), anServiceUri.getPort());
            int aClientPort = (aClientUri.getPort() < 0) ? 0 : aClientUri.getPort();
            myClientEndPoint = new InetSocketAddress(aClientUri.getHost(), aClientPort);
            myOutpuConnectorAddress = outpuConnectorAddress;
            myProtocolFormatter = protocolFormatter;
            myReuseAddressFlag = reuseAddressFlag;
            myTtl = ttl;
            myAllowBroadcastFlag = allowBroadcast;
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
    public void openConnection(IMethod1<MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (responseMessageHandler == null)
            {
                throw new IllegalArgumentException("responseMessageHandler is null.");
            }

            myConnectionManipulatorLock.lock();
            try
            {
                myResponseMessageHandler = responseMessageHandler;

                // Listen on the client address.
                myResponseReceiver = UdpReceiver.createBoundReceiver(myClientEndPoint, myReuseAddressFlag, myTtl, myAllowBroadcastFlag, myMulticastGroup, myMulticastLoopbackFlag);
                myResponseReceiver.startListening(myOnResponseMessageReceived);
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                myResponseMessageHandler = null;

                myResponseReceiver.stopListening();
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        myConnectionManipulatorLock.lock();
        try
        {
            return myResponseReceiver != null && myResponseReceiver.isListening();
        }
        finally
        {
            myConnectionManipulatorLock.unlock();
        }
    }

    @Override
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                byte[] anEncodedMessage = (byte[])myProtocolFormatter.encodeMessage(myOutpuConnectorAddress, message);
                myResponseReceiver.sendTo(anEncodedMessage, myServiceEndpoint);
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(byte[] datagram, InetSocketAddress clientAddress)
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
                    IMethod1<MessageContext> aMessageHandler = myResponseMessageHandler;
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
    private String myOutpuConnectorAddress;
    private InetSocketAddress myClientEndPoint;
    private InetSocketAddress myServiceEndpoint;
    private InetAddress myMulticastGroup;
    private boolean myReuseAddressFlag;
    private int myTtl;
    private boolean myAllowBroadcastFlag;
    private boolean myMulticastLoopbackFlag;
    private IMethod1<MessageContext> myResponseMessageHandler;
    private UdpReceiver myResponseReceiver;
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    
    private IMethod2<byte[], InetSocketAddress> myOnResponseMessageReceived = new IMethod2<byte[], InetSocketAddress>()
    {
        @Override
        public void invoke(byte[] x, InetSocketAddress y) throws Exception
        {
            onResponseMessageReceived(x,y);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
