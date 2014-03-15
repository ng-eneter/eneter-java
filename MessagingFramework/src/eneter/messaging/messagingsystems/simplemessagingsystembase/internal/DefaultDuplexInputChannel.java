/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.*;
import eneter.net.system.linq.internal.EnumerableExt;


public class DefaultDuplexInputChannel implements IDuplexInputChannel
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
    
    
    public DefaultDuplexInputChannel(String channelId,      // address to listen
            IThreadDispatcher dispatcher,                         // threading model used to notify messages and events
            IThreadDispatcher dispatchingAfterMessageReading,
            IInputConnector inputConnector,                 // listener used for listening to messages
            IProtocolFormatter<?> protocolFormatter)        // how messages are encoded between channels
            throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (StringExt.isNullOrEmpty(channelId))
                {
                    EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                    throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
                }
    
                myChannelId = channelId;
                myDispatcher = dispatcher;
                
                // Internal dispatcher used when the message is decoded.
                // E.g. Shared memory messaging needs to return looping immediately the protocol message is decoded
                //      so that other senders are not blocked.
                myDispatchingAfterMessageReading = dispatchingAfterMessageReading;
                
                myProtocolFormatter = protocolFormatter;
                myInputConnector = inputConnector;
                
                myIsIncludeResponseReceiverIdToResponses = false;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
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
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEvent.getApi();
    }

    public void includeResponseReceiverIdToResponses(boolean flag)
    {
        myIsIncludeResponseReceiverIdToResponses = flag;
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }
    
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                // If the channel is already listening.
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Start listen to messages.
                    myInputConnector.startListening(myHandleMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    // The listening did not start correctly.
                    // So try to clean.
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
    
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                myStopListeningIsRunning = true;
                
                try
                {
                    // Try to close connected clients.
                    disconnectClients();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to disconnect connected clients.", err);
                }

                try
                {
                    myInputConnector.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }
                
                myStopListeningIsRunning = false;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myInputConnector.isListening();
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
            
            synchronized (myConnectedClients)
            {
                // Try to find the response sender
                TConnectionContext aConnectionContext = myConnectedClients.get(responseReceiverId);
                if (aConnectionContext == null)
                {
                    String aMessage = TracedObject() + ErrorHandler.SendResponseNotConnectedFailure;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Send the response message.
                    String aResponseReceiverId = myIsIncludeResponseReceiverIdToResponses ? responseReceiverId : "";
                    SenderUtil.sendMessage(aConnectionContext.myResponseSender, aResponseReceiverId, message, myProtocolFormatter);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

                    closeResponseMessageSender(responseReceiverId, true);

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
            closeResponseMessageSender(responseReceiverId, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myDispatcher;
    }

    private void disconnectClients()
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
                    
                    final String aResponseReceiverId = aConnection.getKey();
                    final String aSenderAddreaa = aConnection.getValue().mySenderAddress;
                    myDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            notifyEvent(myResponseReceiverDisconnectedEvent, aResponseReceiverId, aSenderAddreaa);
                        }
                    });
                }
                myConnectedClients.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private boolean handleMessage(final MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageContext == null)
            {
                // If the listening stopped because it was requested.
                if (myStopListeningIsRunning)
                {
                    return false;
                }
                
                // The listening failed.
                EneterTrace.warning(TracedObject() + "detected the listening was stopped.");
                
                // Try to correctly stop the whole listening.
                stopListening();
            }
            
            // Get the protocol message from incoming data.
            ProtocolMessage aProtocolMessage = null;
            
            // if there are message data available.
            if (messageContext.getMessage() != null)
            {
                Object aMessageData = messageContext.getMessage();
                if (aMessageData instanceof InputStream)
                {
                    aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)aMessageData);
                }
                else
                {
                    aProtocolMessage = myProtocolFormatter.decodeMessage(aMessageData);
                }
            }
            
            if (aProtocolMessage == null)
            {
                // Try to correctly close the client.
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            cleanAfterDisconnectedResponseReceiver(messageContext.getResponseSender(), messageContext.getSenderAddress());
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning("Closing of the client failed.", err);
                        }
                    }
                });

                // Stop listening for this client.
                return false;
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                EneterTrace.debug("REQUEST MESSAGE RECEIVED");
                
                final ProtocolMessage aProtocolMessageTmp = aProtocolMessage;
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            // If the connection is not open then it will open it.
                            createResponseMessageSender(messageContext, aProtocolMessageTmp.ResponseReceiverId);
                            
                            myDispatcher.invoke(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    notifyMessageReceived(messageContext, aProtocolMessageTmp);
                                }
                            });
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to create response message sender.", err);
                        }
                    }
                });
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
            {
                EneterTrace.debug("CLIENT CONNECTION RECEIVED");
                
                final ProtocolMessage aProtocolMessageTmp = aProtocolMessage;
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            createResponseMessageSender(messageContext, aProtocolMessageTmp.ResponseReceiverId);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to create response message sender.", err);
                        }
                    }
                });
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                EneterTrace.debug("CLIENT DISCONNECTION RECEIVED");
                
                final String aResponseReceiverId = aProtocolMessage.ResponseReceiverId;
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        closeResponseMessageSender(aResponseReceiverId, false);
                    }
                });
                return false;
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
            }

            return true;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void cleanAfterDisconnectedResponseReceiver(final ISender responseSender, String senderAddress)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Some client got disconnected without sending the 'close' messages.
            // So try to identify closed response receiver and clean it.
            Entry<String, TConnectionContext> aPair;
            synchronized (myConnectedClients)
            {
                aPair = EnumerableExt.firstOrDefault(myConnectedClients.entrySet(), new IFunction1<Boolean, Entry<String, TConnectionContext>>()
                {
                    @Override
                    public Boolean invoke(Entry<String, TConnectionContext> x)
                            throws Exception
                    {
                        return x.getValue().myResponseSender == responseSender;
                    }
                });
            }
    
            if (aPair != null && !StringExt.isNullOrEmpty(aPair.getKey()))
            {
                closeResponseMessageSender(aPair.getKey(), false);
            }
            else
            {
                // Decoding the message returned null.
                // This can happen if a client closed the connection and the stream was closed.
                // e.g. in case of Named Pipes.
    
                // nothing to do here.
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void createResponseMessageSender(MessageContext messageContext, final String responseReceiverId) throws Exception
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
                        aResponseSender = myInputConnector.createResponseSender(responseReceiverId);
                    }

                    aConnectionContext = new TConnectionContext(aResponseSender, messageContext.getSenderAddress());
                    myConnectedClients.put(responseReceiverId, aConnectionContext);

                    // Connection was created.
                    aNewConnectionFlag = true;
                }
            }

            if (aNewConnectionFlag)
            {
                final String aSenderAddress = aConnectionContext.mySenderAddress;
                
                // Notify the connection was open.
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myResponseReceiverConnectedEvent, responseReceiverId, aSenderAddress);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private void closeResponseMessageSender(final String responseReceiverId, boolean sendCloseMessageFlag)
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

                if (aConnectionContext != null)
                {
                    if (sendCloseMessageFlag)
                    {
                        try
                        {
                            // Try to send close connection message.
                            String aResponseReceiverId = myIsIncludeResponseReceiverIdToResponses ? responseReceiverId : "";
                            SenderUtil.sendCloseConnection(aConnectionContext.myResponseSender, aResponseReceiverId, myProtocolFormatter);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                        }
                    }
                    
                    if (aConnectionContext.myResponseSender instanceof IDisposable)
                    {
                        ((IDisposable) aConnectionContext.myResponseSender).dispose();
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to close connection with response receiver.", err);
            }

            // If a connection was closed.
            if (aConnectionContext != null)
            {
                final String aSenderAddress = aConnectionContext.mySenderAddress;
                
                // Notify the connection was closed.
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myResponseReceiverDisconnectedEvent, responseReceiverId, aSenderAddress);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyEvent(EventImpl<ResponseReceiverEventArgs> handler, String responseReceiverId, String senderAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId, senderAddress);
            notifyEventGeneric(handler, aResponseReceiverEvent, false);
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
            DuplexChannelMessageEventArgs aMsg =new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, protocolMessage.ResponseReceiverId, messageContext.getSenderAddress());
            notifyEventGeneric(myMessageReceivedEvent, aMsg, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private <T> void notifyEventGeneric(EventImpl<T> handler, T event, boolean isNobodySubscribedWarning)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler != null)
            {
                try
                {
                    handler.raise(this, event);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else if (isNobodySubscribedWarning)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private HashMap<String, TConnectionContext> myConnectedClients = new HashMap<String, TConnectionContext>();
    
    private IThreadDispatcher myDispatchingAfterMessageReading;
    private IInputConnector myInputConnector;
    private IProtocolFormatter<?> myProtocolFormatter;
    
    private boolean myStopListeningIsRunning;
    private Object myListeningManipulatorLock = new Object();
  
    private String myChannelId;
    private IThreadDispatcher myDispatcher;
    
    private boolean myIsIncludeResponseReceiverIdToResponses;
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEvent = new EventImpl<DuplexChannelMessageEventArgs>();
    
    IFunction1<Boolean, MessageContext> myHandleMessage = new IFunction1<Boolean, MessageContext>()
    {
        @Override
        public Boolean invoke(MessageContext x) throws Exception
        {
            return handleMessage(x);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myChannelId + "' ";
    }
}
