/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod2;

class UdpOutputConnector implements IOutputConnector
{
    public UdpOutputConnector(String ipAddressAndPort, String outpuConnectorAddress, IProtocolFormatter protocolFormatter) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
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
            myOutpuConnectorAddress = outpuConnectorAddress;
            myProtocolFormatter = protocolFormatter;
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
            
            synchronized (myConnectionManipulatorLock)
            {
                try
                {
                    myResponseMessageHandler = responseMessageHandler;
                    myResponseReceiver = new UdpReceiver(myServiceEndpoint, false);
                    myResponseReceiver.startListening(myOnResponseMessageReceived);

                    byte[] anEncodedMessage = (byte[])myProtocolFormatter.encodeOpenConnectionMessage(myOutpuConnectorAddress);
                    DatagramPacket aPacket = new DatagramPacket(anEncodedMessage, anEncodedMessage.length, myServiceEndpoint);
                    myResponseReceiver.getUdpSocket().send(aPacket);
                }
                catch (Exception err)
                {
                    closeConnection();
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
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            cleanConnection(true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulatorLock)
        {
            return myResponseReceiver != null && myResponseReceiver.isListening();
        }
    }

    @Override
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                byte[] anEncodedMessage = (byte[])myProtocolFormatter.encodeMessage(myOutpuConnectorAddress, message);
                DatagramPacket aPacket = new DatagramPacket(anEncodedMessage, anEncodedMessage.length, myServiceEndpoint);
                myResponseReceiver.getUdpSocket().send(aPacket);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onResponseMessageReceived(byte[] datagram, InetSocketAddress dummy)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;

            ProtocolMessage aProtocolMessage = null;
            if (datagram != null)
            {
                aProtocolMessage = myProtocolFormatter.decodeMessage(datagram);
            }
            else
            {
                aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, myOutpuConnectorAddress, null);
            }

            if (aProtocolMessage != null && aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                cleanConnection(false);
            }

            if (aResponseHandler != null)
            {
                try
                {
                    MessageContext aMessageContext = new MessageContext(aProtocolMessage, "");
                    aResponseHandler.invoke(aMessageContext);
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
    
    private void cleanConnection(boolean sendMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                myResponseMessageHandler = null;

                if (myResponseReceiver != null)
                {
                    if (sendMessageFlag)
                    {
                        try
                        {
                            byte[] anEncodedMessage = (byte[])myProtocolFormatter.encodeCloseConnectionMessage(myOutpuConnectorAddress);
                            DatagramPacket aPacket = new DatagramPacket(anEncodedMessage, anEncodedMessage.length, myServiceEndpoint);
                            myResponseReceiver.getUdpSocket().send(aPacket);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to send close connection message.", err);
                        }
                    }

                    myResponseReceiver.stopListening();
                    myResponseReceiver = null;
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
    private InetSocketAddress myServiceEndpoint;
    private IMethod1<MessageContext> myResponseMessageHandler;
    private UdpReceiver myResponseReceiver;
    private Object myConnectionManipulatorLock = new Object();

    private IMethod2<byte[], InetSocketAddress> myOnResponseMessageReceived = new IMethod2<byte[], InetSocketAddress>()
    {
        @Override
        public void invoke(byte[] datagram, InetSocketAddress dummy) throws Exception
        {
            onResponseMessageReceived(datagram, dummy);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
