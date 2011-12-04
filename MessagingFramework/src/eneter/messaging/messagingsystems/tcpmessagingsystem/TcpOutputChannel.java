package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.*;

import eneter.messaging.dataprocessing.streaming.MessageStreamer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.StringExt;

class TcpOutputChannel implements IOutputChannel
{
    public TcpOutputChannel(String ipAddressAndPort) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            try
            {
                // just check if the address is valid
                myUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myChannelId = ipAddressAndPort;
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
                // Creates the socket and connect it to the port.
                Socket aTcpClient = new Socket(InetAddress.getByName(myUri.getHost()), myUri.getPort());

                try
                {
                    // Store the message in the buffer
                    byte[] aBufferedMessage = null;
                    ByteArrayOutputStream aMemStream = new ByteArrayOutputStream();
                    try
                    {
                        MessageStreamer.writeMessage(aMemStream, message);
                        aBufferedMessage = aMemStream.toByteArray();
                    }
                    finally
                    {
                        aMemStream.close();
                    }

                    OutputStream aSendStream = aTcpClient.getOutputStream();

                    // Send the message from the buffer.
                    aSendStream.write(aBufferedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
                finally
                {
                    aTcpClient.getOutputStream().close();
                    aTcpClient.close();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    private URI myUri;
    private Object myLock = new Object();
    
    private String myChannelId;
    
    private String TracedObject()
    {
        return "The Tcp output channel '" + getChannelId() + "' "; 
    }
}
