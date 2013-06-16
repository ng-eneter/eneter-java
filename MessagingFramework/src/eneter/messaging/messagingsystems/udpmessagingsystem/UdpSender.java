/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.ISender;
import eneter.net.system.IMethod1;


class UdpSender implements ISender
{
    public UdpSender(DatagramSocket socket, SocketAddress receiverEndPoint)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySocket = socket;
            myReceiverEndpoint = receiverEndPoint;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
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
            byte[] aMessageData = (byte[])message;
            DatagramPacket aPacket = new DatagramPacket(aMessageData, aMessageData.length, myReceiverEndpoint);
            mySocket.send(aPacket);
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
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support sending via stream.");
    }
    
    private SocketAddress myReceiverEndpoint;
    private DatagramSocket mySocket;
}
