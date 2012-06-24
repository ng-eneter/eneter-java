package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.nio.channels.IllegalSelectorException;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.EProtocolMessageType;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;

class WebSocketDuplexInputChannel extends WebSocketInputChannelBase
                                  implements IDuplexInputChannel
{
    private enum EConnectionState
    {
        Open,
        Closed
    }
    
    private class TClient
    {
        public TClient(IWebSocketClientContext tcpClient)
        {
            myClient = tcpClient;
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

        public IWebSocketClientContext getClient()
        {
            return myClient;
        }
        
        private EConnectionState myConnectionState;

        private IWebSocketClientContext myClient;
    }
    
    
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEvent.getApi();
    }

    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent.getApi();
    }

    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent.getApi();
    }

    
    
    public WebSocketDuplexInputChannel(String ipAddressAndPort, IServerSecurityFactory securityStreamFactory, IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        super(ipAddressAndPort, securityStreamFactory);
        
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
    
    public void sendResponseMessage(String responseReceiverId, Object message)
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
                    Object anEncodedMessage = myProtocolFormatter.encodeMessage("", message);

                    // Send the response message.
                    aClient.getClient().sendMessage(anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

                    try
                    {
                        aClient.getClient().closeConnection();
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
    
    public void disconnectResponseReceiver(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                TClient aClient = myConnectedResponseReceivers.get(responseReceiverId);
                if (aClient != null)
                {
                    aClient.getClient().closeConnection();
                    aClient.setConnectionState(EConnectionState.Closed);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void disconnectClients()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                for (Entry<String, TClient> aConnection : myConnectedResponseReceivers.entrySet())
                {
                    aConnection.getValue().getClient().closeConnection();
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
    protected void handleConnection(IWebSocketClientContext client)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aResponseReceiverId = ""; // will be set when the 1st message is received.

            try
            {
                // While the stop of listening is not requested and the connection is not closed.
                boolean isConnectionClosed = false;
                while (!isConnectionClosed)
                {
                    // Block until a message is received or the connection is closed.
                    WebSocketMessage aWebSocketMessage = client.receiveMessage();
                    if (aWebSocketMessage != null)
                    {
                        ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aWebSocketMessage.getInputStream());

                        if (aProtocolMessage != null)
                        {
                            // If open connection request was received.
                            if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                            {
                                aResponseReceiverId = aProtocolMessage.ResponseReceiverId;

                                synchronized (myConnectedResponseReceivers)
                                {
                                    // Note: It is not allowed that 2 response receivers would have the same responseReceiverId.
                                    if (!myConnectedResponseReceivers.containsKey(aResponseReceiverId))
                                    {
                                        myConnectedResponseReceivers.put(aResponseReceiverId, new TClient(client));
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
                    EConnectionState aConnectionState = EConnectionState.Closed;

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
                    if (aConnectionState == EConnectionState.Open)
                    {
                        ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aResponseReceiverId, null);

                        // Put the message to the queue from where the working thread removes it to notify
                        // subscribers of the input channel.
                        // Note: therfore subscribers of the input channel are notified allways in one thread.
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void handleMessage(ProtocolMessage protocolMessage)
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void notifyResponseReceiverConnected(String responseReceiverId)
    {
        if (myResponseReceiverConnectedEvent.isSubscribed())
        {
            ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

            try
            {
                myResponseReceiverConnectedEvent.raise(this, aResponseReceiverEvent);
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
            if (myResponseReceiverDisconnectedEvent.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

                try
                {
                    myResponseReceiverDisconnectedEvent.raise(this, aResponseReceiverEvent);
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
            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, new DuplexChannelMessageEventArgs(channelId, message, responseReceiverId));
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
    private IProtocolFormatter<?> myProtocolFormatter;
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEvent = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    
    @Override
    protected String TracedObject()
    {
        return "WebSocket duplex input channel '" + getChannelId() + "' "; 
    }
}
