/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.security.InvalidParameterException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.ThreadPool;


class DefaultDuplexInputChannel extends DefaultInputChannelBase implements IDuplexInputChannel
{
    private class TConnectionContext
    {
        public TConnectionContext(ISender responseSender, String senderAddress)
        {
            myResponseSender = responseSender;
            mySenderAddress = senderAddress;
        }

        public String getSenderAddress()
        {
            return mySenderAddress;
        }
        
        public ISender getResponseSender()
        {
            return myResponseSender;
        }
        
        private String mySenderAddress;
        private ISender myResponseSender;
    }
    
    
    public DefaultDuplexInputChannel(String channelId, IMessagingSystemFactory messagingFactory,
                                    IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(channelId))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
            }
            
            myDuplexInputChannelId = channelId;
            myMessagingSystemFactory = messagingFactory;
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
    public String getChannelId()
    {
        return myDuplexInputChannelId;
    }

    @Override
    public void startListening()
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                try
                {
                    myMessageReceiverInputChannel = myMessagingSystemFactory.createInputChannel(myDuplexInputChannelId);
                    myMessageReceiverInputChannel.messageReceived().subscribe(myMessageReceivedHandler);
                    myMessageReceiverInputChannel.startListening();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                    stopListening();
                    
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (myMessageReceiverInputChannel != null)
                {
                    try
                    {
                        myMessageReceiverInputChannel.stopListening();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                    }
                    
                    myMessageReceiverInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myMessageReceiverInputChannel != null && myMessageReceiverInputChannel.isListening();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
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
            
            try
            {
                IOutputChannel aResponseOutputChannel = myMessagingSystemFactory.createOutputChannel(responseReceiverId);
                
                // Encode the response message.
                Object anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
                
                aResponseOutputChannel.sendMessage(anEncodedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
    
                // Sending the response message failed, therefore consider it as the disconnection with the reponse receiver.
                notifyResponseReceiverDisconnected(responseReceiverId);
    
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                IOutputChannel aResponseOutputChannel = myMessagingSystemFactory.createOutputChannel(responseReceiverId);
    
                // Encode the message for closing the connection with the client.
                Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(responseReceiverId);
                
                aResponseOutputChannel.sendMessage(anEncodedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DisconnectResponseReceiverFailure + responseReceiverId, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    @Override
    protected boolean handleMessage(MessageContext messageContext)
            throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    protected void disconnectClients()
    {
        // TODO Auto-generated method stub
        
    }
    
    private void onMessageReceived(Object o, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Decode the incoming message.
                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getMessage());
    
                if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    notifyResponseReceiverConnected(aProtocolMessage.ResponseReceiverId);
                }
                else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                {
                    notifyResponseReceiverDisconnected(aProtocolMessage.ResponseReceiverId);
                }
                else if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                {
                    notifyMessageReceived(getChannelId(), aProtocolMessage.Message, aProtocolMessage.ResponseReceiverId);
                }
                else
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
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
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEventImpl.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId, "");
    
                try
                {
                    myResponseReceiverConnectedEventImpl.raise(this, aResponseReceiverEvent);
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
    
    private void notifyResponseReceiverDisconnected(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId, "");
    
                try
                {
                    myResponseReceiverDisconnectedEventImpl.raise(this, aResponseReceiverEvent);
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
    
    
    private void handleMessage(MessageContext messageContext, ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageContext == null)
            {
                // It means the listening thread stopped looping.
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        stopListening();
                    }
                });
            }
            else if (protocolMessage == null)
            {
                // Some client got disconnected and the reading of the close message failed.
                // Try to find which client it was.
                Entry<String, TConnectionContext> aPair;
                synchronized (myConnectedClients)
                {
                    myConnectedClients.
                    
                    // Note: KeyValuePair is a struct so the default value is not null.
                    aPair = myConnectedClients.FirstOrDefault(x => x.Value.ResponseSender == messageContext.ResponseSender && x.Value.SenderAddress == messageContext.SenderAddress);
                }
                
                if (!String.IsNullOrEmpty(aPair.Key))
                {
                    CloseResponseMessageSender(aPair.Key, true, false);
                }
                else
                {
                    // Decoding the message returned null.
                    // This can happen if a client closed the connection and the stream was closed.
                    // e.g. in case of Named Pipes.
                    
                    // nothing to do here.
                }
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                CloseResponseMessageSender(protocolMessage.ResponseReceiverId, true, false);
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
            {
                CreateResponseMessageSender(messageContext, protocolMessage.ResponseReceiverId);
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                // If the connection is not open then it will open it.
                CreateResponseMessageSender(messageContext, protocolMessage.ResponseReceiverId);

                NotifyMessageReceived(messageContext, protocolMessage);
            }
            else
            {
                EneterTrace.Warning(TracedObject + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void createResponseMessageSender(MessageContext messageContext, String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aNewConnectionFlag = false;

            // If the connection is not open yet.
            TConnectionContext aConnectionContext;
            synchronized (myConnectedClients)
            {
                aConnectionContext = myConnectedClients.get(responseReceiverId);

                if (aConnectionContext == null)
                {
                    // Get the response sender.
                    // It comes in the message context or it must be created.
                    ISender aResponseSender = messageContext.myResponseSender;
                    if (aResponseSender == null)
                    {
                        aResponseSender = myServiceConnector.createResponseSender(responseReceiverId);
                    }

                    aConnectionContext = new TConnectionContext(aResponseSender, messageContext.mySenderAddress);
                    myConnectedClients.put(responseReceiverId, aConnectionContext);

                    aNewConnectionFlag = true;
                }
            }

            if (aNewConnectionFlag)
            {
                // Notify the connection was open.
                notify(myResponseReceiverConnectedEventImpl, responseReceiverId, aConnectionContext.mySenderAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeResponseMessageSender(String responseReceiverId, boolean notifyDisconnectionFalg, boolean sendCloseMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TConnectionContext aConnectionContext = null;

            try
            {
                synchronized (myConnectedClients)
                {
                    aConnectionContext = myConnectedClients.get(responseReceiverId);
                    myConnectedClients.remove(responseReceiverId);
                }

                if (sendCloseMessageFlag && aConnectionContext != null)
                {
                    try
                    {
                        // Try to send close connection message.
                        SenderUtil.sendCloseConnection(aConnectionContext.myResponseSender, "", myProtocolFormatter);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to close connection with response receiver.", err);
            }

            // If a connection was closed.
            if (notifyDisconnectionFalg && aConnectionContext != null)
            {
                // Notify the connection was closed.
                notify(myResponseReceiverDisconnectedEventImpl, responseReceiverId, aConnectionContext.mySenderAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notify(EventImpl<ResponseReceiverEventArgs> handler, String responseReceiverId, String senderAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler.isSubscribed())
            {
                try
                {
                    ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId, senderAddress);
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
    
    private void notifyMessageReceived(MessageContext messageContext, ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, protocolMessage.ResponseReceiverId, messageContext.mySenderAddress));
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
    
    
  
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private HashMap<String, TConnectionContext> myConnectedClients = new HashMap<String, TConnectionContext>();
}
