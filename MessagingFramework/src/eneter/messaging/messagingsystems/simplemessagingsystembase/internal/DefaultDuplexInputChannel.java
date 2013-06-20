/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.*;
import eneter.net.system.linq.internal.EnumerableExt;
import eneter.net.system.threading.internal.ThreadPool;


public class DefaultDuplexInputChannel extends DefaultInputChannelBase implements IDuplexInputChannel
{
    private class TConnectionContext
    {
        public TConnectionContext(ISender responseSender, String senderAddress)
        {
            myResponseSender = responseSender;
            mySenderAddress = senderAddress;
        }

        private String mySenderAddress;
        private ISender myResponseSender;
    }
    
    
    public DefaultDuplexInputChannel(String channelId,
            IInvoker workingThreadInvoker,
            IProtocolFormatter<?> protocolFormatter,
            IServiceConnectorFactory serviceConnectorFactory) throws Exception
        {
            super(channelId, workingThreadInvoker, protocolFormatter, serviceConnectorFactory);
        }
    
    
    @Override
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent.getApi();
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
            
            synchronized (myConnectedClients)
            {
                // Try to find the response sender
                TConnectionContext aConnectionContext = myConnectedClients.get(responseReceiverId);
                if (aConnectionContext == null)
                {
                    String aMessage = TracedObject() + ErrorHandler.CloseConnectionFailure;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Send the response message.
                    SenderUtil.sendMessage(aConnectionContext.myResponseSender, "", message, myProtocolFormatter);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

                    closeResponseMessageSender(responseReceiverId, true, true);

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
    public void disconnectResponseReceiver(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            closeResponseMessageSender(responseReceiverId, true, true);
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
            synchronized (myConnectedClients)
            {
                for (Entry<String, TConnectionContext> aConnection : myConnectedClients.entrySet())
                {
                    if (aConnection.getValue().myResponseSender instanceof IDisposable)
                    {
                        ((IDisposable)aConnection.getValue().myResponseSender).dispose();
                    }
                    
                    notify(myResponseReceiverDisconnectedEvent, aConnection.getKey(), aConnection.getValue().mySenderAddress);
                }
                myConnectedClients.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected boolean handleMessage(final MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = null;

            if (messageContext != null && messageContext.getMessage() != null)
            {
                aProtocolMessage = getProtocolMessage(messageContext.getMessage());
            }
            else
            {
                EneterTrace.warning(TracedObject() + "detected the listening was stopped.");
            }

            // Execute the processing of the message according to desired thread mode.
            final ProtocolMessage aProtocolMessageTmp = aProtocolMessage;
            myWorkingThreadInvoker.invoke(new IMethod()
            {
                @Override
                public void invoke() throws Exception
                {
                    handleMessage(messageContext, aProtocolMessageTmp);
                }
            });

            return aProtocolMessage != null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleMessage(final MessageContext messageContext, ProtocolMessage protocolMessage) throws Exception
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
                    aPair = EnumerableExt.firstOrDefault(myConnectedClients.entrySet(), new IFunction1<Boolean, Entry<String, TConnectionContext>>()
                    {
                        @Override
                        public Boolean invoke(Entry<String, TConnectionContext> x) throws Exception
                        {
                            return x.getValue().myResponseSender == messageContext.getResponseSender() && x.getValue().mySenderAddress.equals(messageContext.getSenderAddress());
                        }
                    });
                }
                
                if (aPair != null && !StringExt.isNullOrEmpty(aPair.getKey()))
                {
                    closeResponseMessageSender(aPair.getKey(), true, false);
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
                closeResponseMessageSender(protocolMessage.ResponseReceiverId, true, false);
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
            {
                createResponseMessageSender(messageContext, protocolMessage.ResponseReceiverId);
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                // If the connection is not open then it will open it.
                createResponseMessageSender(messageContext, protocolMessage.ResponseReceiverId);

                notifyMessageReceived(messageContext, protocolMessage);
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
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
                    ISender aResponseSender = messageContext.getResponseSender();
                    if (aResponseSender == null)
                    {
                        aResponseSender = myServiceConnector.createResponseSender(responseReceiverId);
                    }

                    aConnectionContext = new TConnectionContext(aResponseSender, messageContext.getSenderAddress());
                    myConnectedClients.put(responseReceiverId, aConnectionContext);

                    aNewConnectionFlag = true;
                }
            }

            if (aNewConnectionFlag)
            {
                // Notify the connection was open.
                notify(myResponseReceiverConnectedEvent, responseReceiverId, aConnectionContext.mySenderAddress);
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
                notify(myResponseReceiverDisconnectedEvent, responseReceiverId, aConnectionContext.mySenderAddress);
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
            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, protocolMessage.ResponseReceiverId, messageContext.getSenderAddress()));
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
    
    
  
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEvent = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private HashMap<String, TConnectionContext> myConnectedClients = new HashMap<String, TConnectionContext>();
}
