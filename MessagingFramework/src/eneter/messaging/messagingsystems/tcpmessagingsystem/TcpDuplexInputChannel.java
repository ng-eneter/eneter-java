package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.*;
import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;



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
        
        private EConnectionState myConnectionState;
        private Socket myTcpClient;
        private OutputStream myCommunicationStream;
    }
    
    
    public TcpDuplexInputChannel(String ipAddressAndPort, IProtocolFormatter<byte[]> protocolFormatter)
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
    public void sendResponseMessage(String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
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

                    aClient.getCommunicationStream().close();

                    try
                    {
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

                    // Put the message about the disconnections to the queue from where the working thread removes it to notify
                    // subscribers of the input channel.
                    // Note: therfore subscribers of the input channel are notified allways in one thread.
                    ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, responseReceiverId, null);
                    myMessageProcessingThread.enqueueMessage(aProtocolMessage);

                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                    
                    aClient.getCommunicationStream().close();

                    try
                    {
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
    protected void handleConnection(Socket clientSocket)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                String aResponseReceiverId = ""; // will be set when the 1st message is received.

                InputStream anInputStream = null;

                try
                {
                    // If the end is requested.
                    if (!myStopTcpListeningRequested)
                    {
                        anInputStream = clientSocket.getInputStream();
                        

                        // While the stop of listening is not requested and the connection is not closed.
                        boolean isConnectionClosed = false;
                        while (!myStopTcpListeningRequested && !isConnectionClosed)
                        {
                            // Block until a message is received or the connection is closed.
                            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(anInputStream);

                            if (!myStopTcpListeningRequested)
                            {
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

                                        // Put the message to the queue from where the working thread removes it to notify
                                        // subscribers of the input channel.
                                        // Note: therfore subscribers of the input channel are notified allways in one thread.
                                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                                    }
                                    // If response receiver connection closed message
                                    else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                                    {
                                        isConnectionClosed = true;
                                    }
                                    else
                                    {
                                        // Put the message to the queue from where the working thread removes it to notify
                                        // subscribers of the input channel.
                                        // Note: therfore subscribers of the input channel are notified allways in one thread.
                                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                                    }
                                }
                                else
                                {
                                    isConnectionClosed = true;
                                }
                            }
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
                        if (!myStopTcpListeningRequested && aConnectionState == TClient.EConnectionState.Open)
                        {
                            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aResponseReceiverId, null);

                            // Put the message to the queue from where the working thread removes it to notify
                            // subscribers of the input channel.
                            // Note: therfore subscribers of the input channel are notified allways in one thread.
                            myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                        }
                    }

                    // Close the connection.
                    if (anInputStream != null)
                    {
                        anInputStream.close();
                    }
                    clientSocket.close();
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingTcpConnectionFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingTcpConnectionFailure, err);
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
            try
            {
                if (protocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    notifyResponseReceiverConnected(protocolMessage.ResponseReceiverId);
                }
                else if (protocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                {
                    notifyResponseReceiverDisconnected(protocolMessage.ResponseReceiverId);
                }
                else if (protocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                {
                    notifyMessageReceived(getChannelId(), protocolMessage.Message, protocolMessage.ResponseReceiverId);
                }
                else
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void notifyResponseReceiverConnected(String responseReceiverId)
    {
        if (myResponseReceiverConnectedEventImpl.isSubscribed())
        {
            ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

            try
            {
                myResponseReceiverConnectedEventImpl.update(this, aResponseReceiverEvent);
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
    }
    
    private void notifyResponseReceiverDisconnected(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

                try
                {
                    myResponseReceiverDisconnectedEventImpl.update(this, aResponseReceiverEvent);
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyMessageReceived(String channelId, Object message, String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.update(this, new DuplexChannelMessageEventArgs(channelId, message, responseReceiverId));
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
