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
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod;
import eneter.net.system.internal.StringExt;



class TcpDuplexInputChannel extends TcpInputChannelBase
                            implements IDuplexInputChannel
{
    private static class TClient
    {
        public enum EConnectionState
        {
            Open,
            Closed
        }

        public TClient(Socket tcpClient) throws IOException
        {
            myTcpClient = tcpClient;
            myCommunicationStream = tcpClient.getOutputStream();
            myConnectionState = EConnectionState.Open;
            
            // Get IP address of connected client.
            myClientIp = IpAddressUtil.getRemoteIpAddress(tcpClient);
        }

        public EConnectionState getConnectionState()
        {
            return myConnectionState;
        }
        
        public void setConnectionState(EConnectionState connectionState)
        {
            myConnectionState = connectionState;
        }

        public Socket getTcpClient()
        {
            return myTcpClient;
        }

        public OutputStream getCommunicationStream()
        {
            return myCommunicationStream;
        }
        
        public String getClientIp()
        {
            return myClientIp;
        }
        
        private EConnectionState myConnectionState;
        private Socket myTcpClient;
        private OutputStream myCommunicationStream;
        private String myClientIp;
    }
    
    
    public TcpDuplexInputChannel(String ipAddressAndPort,
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
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEventImpl.getApi();
    }

    @Override
    public void sendResponseMessage(final String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!isListening())
            {
                String aMessage = TracedObject() + ErrorHandler.SendResponseNotListeningFailure;
                EneterTrace.error(aMessage);
                throw new IllegalStateException(aMessage);
            }
            
            TClient aClient;
            synchronized (myConnectedResponseReceivers)
            {
                aClient = myConnectedResponseReceivers.get(responseReceiverId);
            }

            if (aClient != null)
            {
                try
                {
                    // Encode the response message.
                    byte[] anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
                    
                    OutputStream aSendStream = aClient.getCommunicationStream();
                    aSendStream.write(anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

                    try
                    {
                        aClient.getCommunicationStream().close();
                        aClient.getTcpClient().close();
                    }
                    catch (Exception err2)
                    {
                        // do not care if an exception during closing the tcp client.
                    }

                    synchronized (myConnectedResponseReceivers)
                    {
                        myConnectedResponseReceivers.remove(responseReceiverId);
                    }

                    // Notify connection closed from the working thread.
                    final String aClientIp = aClient.getClientIp();
                    myMessageProcessingWorker.invoke(new IMethod()
                    {
                        @Override
                        public void invoke() throws Exception
                        {
                            notifyEvent(myResponseReceiverDisconnectedEventImpl, responseReceiverId, aClientIp);
                        }
                    });

                    throw err;
                }
            }
            else
            {
                String anError = TracedObject() + ErrorHandler.SendMessageNotConnectedFailure;
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                TClient aClient = myConnectedResponseReceivers.get(responseReceiverId);
                if (aClient != null)
                {
                    aClient.getCommunicationStream().close();
                    aClient.getTcpClient().close();
                    aClient.setConnectionState(TClient.EConnectionState.Closed);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void disconnectClients() throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                for (TClient aConnection : myConnectedResponseReceivers.values())
                {
                    aConnection.getCommunicationStream().close();
                    aConnection.getTcpClient().close();
                }
                myConnectedResponseReceivers.clear();
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
            // Get IP address of connected client.
            final String aClientIp = IpAddressUtil.getRemoteIpAddress(clientSocket);
            
            String aResponseReceiverId = ""; // will be set when the 1st message is received.

            InputStream anInputStream = null;

            try
            {
                anInputStream = clientSocket.getInputStream();
                

                // While the stop of listening is not requested and the connection is not closed.
                boolean isConnectionClosed = false;
                while (!isConnectionClosed)
                {
                    // Block until a message is received or the connection is closed.
                    final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(anInputStream);

                    if (aProtocolMessage != null)
                    {
                        // If response receiver connection open message
                        if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                        {
                            aResponseReceiverId = aProtocolMessage.ResponseReceiverId;
                            
                            synchronized (myConnectedResponseReceivers)
                            {
                                // Note: It is not allowed that 2 response receivers would have the same responseReceiverId.
                                if (!myConnectedResponseReceivers.containsKey(aResponseReceiverId))
                                {
                                    myConnectedResponseReceivers.put(aResponseReceiverId, new TClient(clientSocket));
                                }
                                else
                                {
                                    throw new IllegalStateException("The resposne receiver '" + aResponseReceiverId + "' is already connected. It is not allowed, that response receivers share the same id.");
                                }
                            }

                            // Notify open connection from the working thread.
                            myMessageProcessingWorker.invoke(new IMethod()
                            {
                                @Override
                                public void invoke() throws Exception
                                {
                                    notifyEvent(myResponseReceiverConnectedEventImpl, aProtocolMessage.ResponseReceiverId, aClientIp);
                                }
                            });
                        }
                        // If response receiver connection closed message
                        else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                        {
                            isConnectionClosed = true;
                        }
                        else if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                        {
                            // Notify the message received from the working thread.
                            myMessageProcessingWorker.invoke(new IMethod()
                            {
                                @Override
                                public void invoke() throws Exception
                                {
                                    notifyMessageReceived(aProtocolMessage.Message, aProtocolMessage.ResponseReceiverId, aClientIp);
                                }
                            });
                        }
                        else
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                        }
                    }
                    else
                    {
                        isConnectionClosed = true;
                    }
                }
            }
            finally
            {
                if (!StringExt.isNullOrEmpty(aResponseReceiverId))
                {
                    TClient.EConnectionState aConnectionState = TClient.EConnectionState.Closed;

                    synchronized (myConnectedResponseReceivers)
                    {
                        TClient aTClient = myConnectedResponseReceivers.get(aResponseReceiverId);
                        if (aTClient != null)
                        {
                            aConnectionState = aTClient.getConnectionState();
                        }

                        myConnectedResponseReceivers.remove(aResponseReceiverId);
                    }

                    // If the connection was not closed from this duplex input channel (i.e. by stopping of listener
                    // or by calling 'DisconnectResponseReceiver()', then notify, that the client disconnected itself.
                    if (aConnectionState == TClient.EConnectionState.Open)
                    {
                        final String aReceiverId = aResponseReceiverId; 
                        
                        // Notify the connection closed from the working thread.
                        myMessageProcessingWorker.invoke(new IMethod()
                        {
                            @Override
                            public void invoke() throws Exception
                            {
                                notifyEvent(myResponseReceiverDisconnectedEventImpl, aReceiverId, aClientIp);
                            }
                        });
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

 
    private void notifyEvent(EventImpl<ResponseReceiverEventArgs> handler, String responseReceiverId, String clientAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId, clientAddress);

                try
                {
                    handler.raise(this, aResponseReceiverEvent);
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
    
    private void notifyMessageReceived(Object message, String responseReceiverId, String clientAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new DuplexChannelMessageEventArgs(myChannelId, message, responseReceiverId, clientAddress));
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
    
    
    
    
    private HashMap<String, TClient> myConnectedResponseReceivers = new HashMap<String, TClient>();
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    
    @Override
    protected String TracedObject()
    {
        return "Tcp duplex input channel '" + getChannelId() + "' "; 
    }

}
