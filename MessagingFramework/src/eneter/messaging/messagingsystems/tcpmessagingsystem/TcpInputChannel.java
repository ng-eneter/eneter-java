package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.*;
import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;

class TcpInputChannel extends TcpInputChannelBase
                      implements IInputChannel
{
    public TcpInputChannel(String ipAddressAndPort, IProtocolFormatter<byte[]> protocolFormatter)
            throws Exception
    {
        super(ipAddressAndPort);
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventApi;
    }


    @Override
    protected void disconnectClients() throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedSenders)
            {
                for (Socket aConnection : myConnectedSenders)
                {
                    aConnection.close();
                }
                myConnectedSenders.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void handleConnection(Socket clientSocket)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                synchronized (myConnectedSenders)
                {
                    myConnectedSenders.add(clientSocket);
                }

                try
                {
                    // If the end is requested.
                    if (!myStopTcpListeningRequested)
                    {
                        // Source stream.
                        InputStream anInputStream = clientSocket.getInputStream();

                        // First read the message to the buffer.
                        ProtocolMessage aProtocolMessage = null;
                        ByteArrayOutputStream anOutputMemStream = new ByteArrayOutputStream();
                        try
                        {
                            int aSize = 0;
                            byte[] aBuffer = new byte[32768];
                            while ((aSize = anInputStream.read(aBuffer, 0, aBuffer.length)) != -1)
                            {
                                anOutputMemStream.write(aBuffer, 0, aSize);
                            }
                        }
                        finally
                        {
                            anOutputMemStream.close();
                        }

                        // Decode the incoming message.
                        aProtocolMessage = myProtocolFormatter.decodeMessage(anOutputMemStream.toByteArray());

                        if (!myStopTcpListeningRequested && aProtocolMessage != null)
                        {
                            // Put the message to the queue from where the working thread removes it to notify
                            // subscribers of the input channel.
                            // Note: therfore subscribers of the input channel are notified allways in one thread.
                            myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                        }
                    }
                }
                finally
                {
                    synchronized (myConnectedSenders)
                    {
                        myConnectedSenders.remove(clientSocket);
                    }

                    // Close the connection.
                    clientSocket.close();
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingHttpConnectionFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingHttpConnectionFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void messageHandler(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }
            
            if (myMessageReceivedEventImpl.isEmpty() == false)
            {
                try
                {
                    myMessageReceivedEventImpl.update(this, new ChannelMessageEventArgs(getChannelId(), protocolMessage.Message));
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                    throw err;
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private ArrayList<Socket> myConnectedSenders = new ArrayList<Socket>();
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
    private Event<ChannelMessageEventArgs> myMessageReceivedEventApi = new Event<ChannelMessageEventArgs>(myMessageReceivedEventImpl);
    
    @Override
    protected String TracedObject()
    {
        return "Tcp input channel '" + getChannelId() + "' "; 
    }

}
