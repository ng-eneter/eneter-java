/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.internal.StringExt;

class TcpOutputChannel implements IOutputChannel
{
    public TcpOutputChannel(String ipAddressAndPort, IProtocolFormatter<byte[]> protocolFormatter,
            IClientSecurityFactory clientSecurityFactory)
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
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            
            mySocketAddress = new InetSocketAddress(aUri.getHost(), aUri.getPort());

            myChannelId = ipAddressAndPort;
            myProtocolFormatter = protocolFormatter;
            myClientSecurityFactory = clientSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public String getChannelId()
    {
        return myChannelId;
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                try
                {
                    // Creates the socket and connect it to the port.
                    //Socket aTcpClient = new Socket(InetAddress.getByName(myUri.getHost()), myUri.getPort());
                    //aTcpClient.setTcpNoDelay(true);
                    Socket aTcpClient = myClientSecurityFactory.createClientSocket(mySocketAddress);
                    
                    try
                    {
                        // Encode the message.
                        byte[] anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
                        
                        OutputStream aSendStream = aTcpClient.getOutputStream();

                        // Send the message from the buffer.
                        aSendStream.write(anEncodedMessage);
                    }
                    finally
                    {
                        aTcpClient.getOutputStream().close();
                        aTcpClient.close();
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IClientSecurityFactory myClientSecurityFactory;
    private InetSocketAddress mySocketAddress;
    private Object myLock = new Object();
    
    private String myChannelId;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    private String TracedObject()
    {
        return "The Tcp output channel '" + getChannelId() + "' "; 
    }
}
