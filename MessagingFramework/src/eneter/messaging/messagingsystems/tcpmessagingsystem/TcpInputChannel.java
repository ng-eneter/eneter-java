package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.*;
import java.util.*;

import eneter.messaging.dataprocessing.streaming.MessageStreamer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;

class TcpInputChannel extends TcpInputChannelBase
                      implements IInputChannel
{
    public TcpInputChannel(String ipAddressAndPort) throws Exception
    {
        super(ipAddressAndPort);
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
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
                        Object aMessage = null;
                        
                        BufferedInputStream aMemStream = new BufferedInputStream(anInputStream);
                        try
                        {
                            aMessage = MessageStreamer.readMessage(aMemStream);
                        }
                        finally
                        {
                            aMemStream.close();
                        }
                        
                        if (!myStopTcpListeningRequested && aMessage != null)
                        {
                            // Put the message to the queue from where the working thread removes it to notify
                            // subscribers of the input channel.
                            // Note: therfore subscribers of the input channel are notified allways in one thread.
                            myMessageProcessingThread.enqueueMessage(aMessage);
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void messageHandler(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isEmpty() == false)
            {
                try
                {
                    myMessageReceivedEventImpl.update(this, new ChannelMessageEventArgs(getChannelId(), message));
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
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
    private Event<ChannelMessageEventArgs> myMessageReceivedEventApi = new Event<ChannelMessageEventArgs>(myMessageReceivedEventImpl);
    
    @Override
    protected String TracedObject()
    {
        return "Tcp input channel '" + getChannelId() + "' "; 
    }

}
