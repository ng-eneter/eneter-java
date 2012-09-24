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
import java.util.*;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.dataprocessing.streaming.internal.StreamUtil;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod;

class TcpInputChannel extends TcpInputChannelBase implements IInputChannel
{
    public TcpInputChannel(String ipAddressAndPort,
            IInvoker invoker,
            IProtocolFormatter<byte[]> protocolFormatter,
            IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        super(ipAddressAndPort, invoker, serverSecurityFactory);
        
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
        return myMessageReceivedEventImpl.getApi();
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
    protected void handleConnection(Socket clientSocket) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedSenders)
            {
                myConnectedSenders.add(clientSocket);
            }

            try
            {
                // Get IP address of connected client.
                final String aClientIp = IpAddressUtil.getRemoteIpAddress(clientSocket);
                
                // Source stream.
                InputStream anInputStream = clientSocket.getInputStream();

                // Get the protocol message.
                byte[] aMessageData = StreamUtil.readToEnd(anInputStream);
                final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aMessageData);
                
                if (aProtocolMessage != null)
                {
                    if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                    {
                        // Notify message received from the working thread.
                        myMessageProcessingWorker.invoke(new IMethod()
                        {
                            @Override
                            public void invoke() throws Exception
                            {
                                notifyMessageReceived(aProtocolMessage.Message, aClientIp);
                            }
                        });
                    }
                    else
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
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
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyMessageReceived(Object message, String clientAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new ChannelMessageEventArgs(getChannelId(), message, clientAddress));
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
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
    
    @Override
    protected String TracedObject()
    {
        return "Tcp input channel '" + getChannelId() + "' "; 
    }

}
