/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.io.OutputStream;
import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;

class UdpOutputConnector implements IOutputConnector
{
    public UdpOutputConnector(String ipAddressAndPort) throws Exception
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void openConnection(
            IFunction1<Boolean, MessageContext> responseMessageHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (responseMessageHandler != null)
                {
                    myResponseReceiver = new UdpReceiver(myServiceEndpoint, false);
                    myResponseReceiver.startListening(responseMessageHandler);

                    // Get connected socket.
                    myClientSocket = myResponseReceiver.getUdpSocket();
                }
                else
                {
                    myClientSocket = new DatagramSocket();
                    myClientSocket.connect(myServiceEndpoint);
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
            synchronized (myConnectionManipulatorLock)
            {
                if (myResponseReceiver != null)
                {
                    myResponseReceiver.stopListening();
                    myResponseReceiver = null;
                }

                myServiceEndpoint = null;

                if (myClientSocket != null)
                {
                    myClientSocket.close();
                    myClientSocket = null;
                }
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
        synchronized (myConnectionManipulatorLock)
        {
            // If one-way communication.
            if (myResponseReceiver == null)
            {
                return myClientSocket != null;
            }

            return myResponseReceiver.isListening();
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
            synchronized (myConnectionManipulatorLock)
            {
                if (!isConnected())
                {
                    throw new IllegalStateException(TracedObject() + ErrorHandler.SendMessageNotConnectedFailure);
                }

                byte[] aMessageData = (byte[])message;
                DatagramPacket aPacket = new DatagramPacket(aMessageData, aMessageData.length, myServiceEndpoint);
                myClientSocket.send(aPacket);
            }
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
        throw new IllegalStateException("toStreamWritter is not supported.");
    }

    
    private InetSocketAddress myServiceEndpoint;
    private DatagramSocket myClientSocket;
    private UdpReceiver myResponseReceiver;
    private Object myConnectionManipulatorLock = new Object();

    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}
