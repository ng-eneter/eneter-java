/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.*;


public class DefaultDuplexInputChannel implements IDuplexInputChannel
{
    public DefaultDuplexInputChannel(String channelId,      // address to listen
            IThreadDispatcher dispatcher,                         // threading model used to notify messages and events
            IThreadDispatcher dispatchingAfterMessageReading,
            IInputConnector inputConnector)        // how messages are encoded between channels
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
            
            myInputConnector = inputConnector;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnected.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnected.getApi();
    }
    
    @Override
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceived.getApi();
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
            myListeningManipulatorLock.lock();
            try
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
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToStartListening, err);

                    // The listening did not start correctly.
                    // So try to clean.
                    stopListening();

                    throw err;
                }
            }
            finally
            {
                myListeningManipulatorLock.unlock();
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
            myListeningManipulatorLock.lock();
            try
            {
                try
                {
                    myInputConnector.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.IncorrectlyStoppedListening, err);
                }
            }
            finally
            {
                myListeningManipulatorLock.unlock();
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
            myListeningManipulatorLock.lock();
            try
            {
                return myInputConnector.isListening();
            }
            finally
            {
                myListeningManipulatorLock.unlock();
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
            if (StringExt.isNullOrEmpty(responseReceiverId))
            {
                String aMessage = TracedObject() + "detected the input parameter responseReceiverId is null or empty string.";
                EneterTrace.error(aMessage);
                throw new IllegalArgumentException(aMessage);
            }
            
            if (!isListening())
            {
                String aMessage = TracedObject() + ErrorHandler.FailedToSendResponseBecauseNotListening;
                EneterTrace.error(aMessage);
                throw new IllegalStateException(aMessage);
            }
            
            // If broadcast to all connected response receivers.
            if (responseReceiverId.equals("*"))
            {
                myInputConnector.sendBroadcast(message);
            }
            else
            {
                try
                {
                    // Send the response message.
                    myInputConnector.sendResponseMessage(responseReceiverId, message);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);
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
            try
            {
                myInputConnector.closeConnection(responseReceiverId);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
            }
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

    private void handleMessage(final MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageContext.getProtocolMessage().MessageType == EProtocolMessageType.MessageReceived)
            {
                EneterTrace.debug("REQUEST MESSAGE RECEIVED");

                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myDispatcher.invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                notifyMessageReceived(messageContext, messageContext.getProtocolMessage());
                            }
                        });
                    }
                });
            }
            else if (messageContext.getProtocolMessage().MessageType == EProtocolMessageType.OpenConnectionRequest)
            {
                EneterTrace.debug("CLIENT CONNECTION RECEIVED");
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myDispatcher.invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                notifyEvent(myResponseReceiverConnected, messageContext.getProtocolMessage().ResponseReceiverId, messageContext.getSenderAddress());
                            }
                        });
                    }
                });
            }
            else if (messageContext.getProtocolMessage().MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                EneterTrace.debug("CLIENT DISCONNECTION RECEIVED");
                myDispatchingAfterMessageReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myDispatcher.invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                notifyEvent(myResponseReceiverDisconnected, messageContext.getProtocolMessage().ResponseReceiverId, messageContext.getSenderAddress());
                            }
                        });
                    }
                });
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToReceiveMessageBecauseIncorrectFormat);
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
            notifyEventGeneric(myMessageReceived, aMsg, true);
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
    
    
    private IThreadDispatcher myDispatchingAfterMessageReading;
    private IInputConnector myInputConnector;
    
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
  
    private String myChannelId;
    private IThreadDispatcher myDispatcher;
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnected = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnected = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceived = new EventImpl<DuplexChannelMessageEventArgs>();
    
    private IMethod1<MessageContext> myHandleMessage = new IMethod1<MessageContext>()
    {
        @Override
        public void invoke(MessageContext x) throws Exception
        {
            handleMessage(x);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myChannelId + "' ";
    }
}
