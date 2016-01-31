/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.linq.internal.EnumerableExt;
import eneter.net.system.threading.internal.EneterTimer;


class MonitoredDuplexInputChannel implements IDuplexInputChannel
{
    private class TResponseReceiverContext
    {
        public TResponseReceiverContext(String responseReceiverId, String clientAddress)
        {
            myResponseReceiverId = responseReceiverId;
            myClientAddress = clientAddress;
            myLastReceiveTime = System.currentTimeMillis();
            myLastPingSentTime = System.currentTimeMillis();
        }

        public String getResponseReceiverId()
        {
            return myResponseReceiverId;
        }
        
        public String getClientAddress()
        {
            return myClientAddress;
        }
        
        public void setLastReceiveTime(long time)
        {
            myLastReceiveTime = time;
        }
        
        public long getLastReceiveTime()
        {
            return myLastReceiveTime;
        }
        
        public void setLastPingSentTime(long time)
        {
            myLastPingSentTime = time;
        }
        
        public long getLastPingSentTime()
        {
            return myLastPingSentTime;
        }
        
        private String myResponseReceiverId;
        private String myClientAddress;
        public long myLastReceiveTime;
        public long myLastPingSentTime;
    }
    
    
    public MonitoredDuplexInputChannel(IDuplexInputChannel underlyingInputChannel, ISerializer serializer,
            long pingFrequency,
            long receiveTimeout,
            IThreadDispatcher dispatcher) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingInputChannel = underlyingInputChannel;
            mySerializer = serializer;
            myPingFrequency = pingFrequency;
            myReceiveTimeout = receiveTimeout;
            myDispatcher = dispatcher;
            
            myCheckTimer = new EneterTimer(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        onCheckerTick();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed when checking timeouted connections.");
                    }
                }
            }, "Eneter.ServiceMonitorReceiveTimer");

            MonitorChannelMessage aPingMessage = new MonitorChannelMessage(MonitorChannelMessageType.Ping, null);
            myPreserializedPingMessage = mySerializer.serialize(aPingMessage, MonitorChannelMessage.class);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    
    
    @Override
    public String getChannelId()
    {
        return myUnderlyingInputChannel.getChannelId();
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myDispatcher;
    }



    @Override
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myListeningManipulatorLock.lock();
            try
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myUnderlyingInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
                myUnderlyingInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
                myUnderlyingInputChannel.messageReceived().subscribe(myOnMessageReceived);

                try
                {
                    myUnderlyingInputChannel.startListening();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToStartListening, err);
                    stopListening();
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

    @Override
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
                    myUnderlyingInputChannel.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.IncorrectlyStoppedListening, err);
                }

                myUnderlyingInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
                myUnderlyingInputChannel.responseReceiverDisconnected().unsubscribe(myOnResponseReceiverDisconnected);
                myUnderlyingInputChannel.messageReceived().unsubscribe(myOnMessageReceived);
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
    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myListeningManipulatorLock.lock();
            try
            {
                return myUnderlyingInputChannel != null && myUnderlyingInputChannel.isListening();
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
            try
            {
                // Create the response message for the monitor duplex output chanel.
                MonitorChannelMessage aMessage = new MonitorChannelMessage(MonitorChannelMessageType.Message, message);
                Object aSerializedMessage = mySerializer.serialize(aMessage, MonitorChannelMessage.class);

                // Send the response message via the underlying channel.
                myUnderlyingInputChannel.sendResponseMessage(responseReceiverId, aSerializedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(final String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myResponseReceiverContextsLock.lock();
                try
                {
                    HashSetExt.removeWhere(myResponseReceiverContexts, new IFunction1<Boolean, TResponseReceiverContext>()
                    {
                        @Override
                        public Boolean invoke(TResponseReceiverContext x)
                                throws Exception
                        {
                            return x.getResponseReceiverId().equals(responseReceiverId);
                        }
                    });
                }
                finally
                {
                    myResponseReceiverContextsLock.unlock();
                }
                
                myUnderlyingInputChannel.disconnectResponseReceiver(responseReceiverId);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToDisconnectResponseReceiver + responseReceiverId, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverConnected(Object sender, final ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiverContextsLock.lock();
            try
            {
                TResponseReceiverContext aResponseReceiver = getResponseReceiver(e.getResponseReceiverId());
                if (aResponseReceiver != null)
                {
                    EneterTrace.warning(TracedObject() + "received open connection from already connected response receiver.");
                    return;
                }
                else
                {
                    createResponseReceiver(e.getResponseReceiverId(), e.getSenderAddress());
                }
            }
            finally
            {
                myResponseReceiverContextsLock.unlock();
            }

            myDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEventGeneric(myResponseReceiverConnectedEventImpl, e, false);
                }
            });
        }
        catch (Exception err)
        {
            EneterTrace.warning(TracedObject() + "detected exception when response receiver connected.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverDisconnected(Object sender, final ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            int aNumberOfRemoved = 0;
            myResponseReceiverContextsLock.lock();
            try
            {
                try
                {
                    aNumberOfRemoved = HashSetExt.removeWhere(myResponseReceiverContexts,
                            new IFunction1<Boolean, TResponseReceiverContext>()
                    {
                        @Override
                        public Boolean invoke(TResponseReceiverContext x)
                                throws Exception
                        {
                            return x.getResponseReceiverId().equals(e.getResponseReceiverId());
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed in removeWhere to remove the response receiver.");
                }
            }
            finally
            {
                myResponseReceiverContextsLock.unlock();
            }

            if (aNumberOfRemoved > 0)
            {
                // Notify response receiver disconnected.
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEventGeneric(myResponseReceiverDisconnectedEventImpl, e, false);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myResponseReceiverContextsLock.lock();
                try
                {
                    TResponseReceiverContext aResponseReceiver = getResponseReceiver(e.getResponseReceiverId());
                    if (aResponseReceiver == null)
                    {
                        // Note: the response receiver was just disconnected.
                        return;
                    }

                    aResponseReceiver.setLastReceiveTime(System.currentTimeMillis());
                }
                finally
                {
                    myResponseReceiverContextsLock.unlock();
                }
                
                // Deserialize the incoming message.
                MonitorChannelMessage aMessage = mySerializer.deserialize(e.getMessage(), MonitorChannelMessage.class);

                // if the message is ping, then response.
                if (aMessage.MessageType == MonitorChannelMessageType.Message)
                {
                    // Notify the incoming message.
                    if (myMessageReceivedEventImpl.isSubscribed())
                    {
                        final DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.MessageContent, e.getResponseReceiverId(), e.getSenderAddress());
                        myDispatcher.invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                notifyEventGeneric(myMessageReceivedEventImpl, aMsg, true);
                            }
                        });
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.FailedToReceiveMessage, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onCheckerTick()
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<TResponseReceiverContext> aPingNeededReceivers = new ArrayList<TResponseReceiverContext>();
            final ArrayList<TResponseReceiverContext> aTimeoutedResponseReceivers = new ArrayList<TResponseReceiverContext>();
            boolean aContinueTimerFlag = false;

            myResponseReceiverContextsLock.lock();
            try
            {
                final long aCurrentTime = System.currentTimeMillis();

                HashSetExt.removeWhere(myResponseReceiverContexts,
                        new IFunction1<Boolean, TResponseReceiverContext>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiverContext x)
                                    throws Exception
                            {
                                if (aCurrentTime - x.getLastReceiveTime() > myReceiveTimeout)
                                {
                                    // Store the timeouted response receiver.
                                    aTimeoutedResponseReceivers.add(x);

                                    // Indicate, that the response receiver can be removed.
                                    return true;
                                }
                                
                                if (aCurrentTime - x.getLastPingSentTime() >= myPingFrequency)
                                {
                                    aPingNeededReceivers.add(x);
                                }

                                // Indicate, that the response receiver cannot be removed.
                                return false;
                            }
                        });

                aContinueTimerFlag = myResponseReceiverContexts.size() > 0;
            }
            finally
            {
                myResponseReceiverContextsLock.unlock();
            }

            // Send pings to all receivers which need it.
            for (TResponseReceiverContext aResponseReceiver : aPingNeededReceivers)
            {
                try
                {
                    myUnderlyingInputChannel.sendResponseMessage(aResponseReceiver.getResponseReceiverId(), myPreserializedPingMessage);
                    aResponseReceiver.setLastPingSentTime(System.currentTimeMillis());
                }
                catch (Exception err)
                {
                    // The sending of ping failed. It means the response receiver will be notified as disconnected.
                }
            }
            
            // Close all removed response receivers.
            for (TResponseReceiverContext aResponseReceiver : aTimeoutedResponseReceivers)
            {
                // Try to disconnect the response receiver.
                try
                {
                    disconnectResponseReceiver(aResponseReceiver.getResponseReceiverId());
                }
                catch (Exception err)
                {
                    // The response receiver is already disconnected, therefore the attempt
                    // to send a message about the disconnection failed.
                }

                // Notify that the response receiver was disconected.
                final ResponseReceiverEventArgs e = new ResponseReceiverEventArgs(aResponseReceiver.getResponseReceiverId(), aResponseReceiver.getClientAddress());
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEventGeneric(myResponseReceiverDisconnectedEventImpl, e, false);
                    }
                });
            }

            // If the timer chall continue.
            if (aContinueTimerFlag)
            {
                myCheckTimer.change(myPingFrequency);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private TResponseReceiverContext getResponseReceiver(final String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TResponseReceiverContext aResponseReceiverContext = null;
            try
            {
                aResponseReceiverContext = EnumerableExt.firstOrDefault(myResponseReceiverContexts,
                    new IFunction1<Boolean, TResponseReceiverContext>()
                    {
                        @Override
                        public Boolean invoke(TResponseReceiverContext x)
                                throws Exception
                        {
                            return x.getResponseReceiverId().equals(responseReceiverId);
                        }
                    });
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed in firstOrDefault to find the response receiver.", err);
            }

            return aResponseReceiverContext;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private TResponseReceiverContext createResponseReceiver(String responseReceiverId, String clientAddress)
            throws InterruptedException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TResponseReceiverContext aResponseReceiver = new TResponseReceiverContext(responseReceiverId, clientAddress);
            myResponseReceiverContexts.add(aResponseReceiver);

            if (myResponseReceiverContexts.size() == 1)
            {
                myCheckTimer.change(myPingFrequency);
            }

            return aResponseReceiver;
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
    
    
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    private IDuplexInputChannel myUnderlyingInputChannel;
    private ISerializer mySerializer;

    private IThreadDispatcher myDispatcher;
    private long myPingFrequency;
    private long myReceiveTimeout;
    private EneterTimer myCheckTimer;
    private ThreadLock myResponseReceiverContextsLock = new ThreadLock();
    private HashSet<TResponseReceiverContext> myResponseReceiverContexts = new HashSet<TResponseReceiverContext>();
    
    private Object myPreserializedPingMessage;
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object x, ResponseReceiverEventArgs y)
        {
            onResponseReceiverConnected(x, y);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object x, ResponseReceiverEventArgs y)
        {
            onResponseReceiverDisconnected(x, y);
        }
    };
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object x, DuplexChannelMessageEventArgs y)
        {
            onMessageReceived(x, y);
        }
    };
    
    private String TracedObject()
    {
        String aChannelId = (myUnderlyingInputChannel != null) ? myUnderlyingInputChannel.getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }
}
