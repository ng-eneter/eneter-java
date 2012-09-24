/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.EProtocolMessageType;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod;
import eneter.net.system.internal.StringExt;

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
            
            // Get IP address of connected client.
            InetSocketAddress aSocketAddress = tcpClient.getClientEndPoint();
            myClientIp = IpAddressUtil.getIpAddress(aSocketAddress);
            
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
        
        public String getClientIp()
        {
            return myClientIp;
        }
        
        private EConnectionState myConnectionState;
        private IWebSocketClientContext myClient;
        private String myClientIp;
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

    
    
    public WebSocketDuplexInputChannel(String ipAddressAndPort,
            IInvoker invoker,
            IServerSecurityFactory securityStreamFactory, IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        super(ipAddressAndPort, invoker, securityStreamFactory);
        
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

                    // Notify connection closed from the working thread.
                    final String aClientIp = aClient.getClientIp();
                    myMessageProcessingWorker.invoke(new IMethod()
                    {
                        @Override
                        public void invoke() throws Exception
                        {
                            notifyEvent(myResponseReceiverDisconnectedEvent, responseReceiverId, aClientIp);
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
            // Get IP address of connected client.
            InetSocketAddress aSocketAddress = client.getClientEndPoint();
            final String aClientIp = IpAddressUtil.getIpAddress(aSocketAddress); 
            
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
                        final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aWebSocketMessage.getInputStream());

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

                                // Notify open connection from the working thread.
                                myMessageProcessingWorker.invoke(new IMethod()
                                {
                                    @Override
                                    public void invoke() throws Exception
                                    {
                                        notifyEvent(myResponseReceiverConnectedEvent, aProtocolMessage.ResponseReceiverId, aClientIp);
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
                        final String aReceiverId = aResponseReceiverId;
                        
                        // Notify the connection closed from the working thread.
                        myMessageProcessingWorker.invoke(new IMethod()
                        {
                            @Override
                            public void invoke() throws Exception
                            {
                                notifyEvent(myResponseReceiverDisconnectedEvent, aReceiverId, aClientIp);
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
            if (handler != null)
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
            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, new DuplexChannelMessageEventArgs(myChannelId, message, responseReceiverId, clientAddress));
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
