/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.*;
import eneter.net.system.linq.internal.EnumerableExt;

class BufferedDuplexInputChannel implements IBufferedDuplexInputChannel
{
    private class TBufferedResponseReceiver
    {
        public TBufferedResponseReceiver(String responseReceiverId, IDuplexInputChannel duplexInputChannel)
        {
            ResponseReceiverId = responseReceiverId;

            // Note: at the time of instantiation the client address does not have to be known.
            //       E.g. if sending response to a not yet connected response receiver.
            //       Therefore it will be set explicitly.
            myClientAddress = "";
            myDuplexInputChannel = duplexInputChannel;
            setOnline(false);
        }

        public void setOnline(boolean value)
        {
            myIsOnline = value;

            if (!myIsOnline)
            {
                myOfflineStartedAt = System.currentTimeMillis();
            }
        }
        
        public boolean isOnline()
        {
            return myIsOnline;
        }

        public void setPendingResponseReceiverConnectedEvent(boolean value)
        {
            myPendingResponseReceiverConnectedEvent = value;
        }
        public boolean getPendingResponseReceiverConnectedEvent()
        {
            return myPendingResponseReceiverConnectedEvent;
        }

        public void sendResponseMessage(Object message)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myMessageQueue.add(message);
                sendMessagesFromQueue();
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void sendMessagesFromQueue()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (myIsOnline)
                {
                    while (myMessageQueue.size() > 0)
                    {
                        Object aMessage = myMessageQueue.peek();
                        try
                        {
                            myDuplexInputChannel.sendResponseMessage(ResponseReceiverId, aMessage);

                            // Message was successfully sent therefore it can be removed from the queue.
                            myMessageQueue.poll();
                        }
                        catch (Exception err)
                        {
                            // Sending failed because of disconnection.
                            setOnline(false);
                            break;
                        }
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public long getOfflineStartedAt()
        {
            return myOfflineStartedAt;
        }
        
        public void setClientAddress(String clientAddress)
        {
            myClientAddress = clientAddress;
        }
        
        public String getClientAddress()
        {
            return myClientAddress;
        }

        public final String ResponseReceiverId;

        private IDuplexInputChannel myDuplexInputChannel;
        private ArrayDeque<Object> myMessageQueue = new ArrayDeque<Object>();
        private boolean myIsOnline;
        
        private boolean myPendingResponseReceiverConnectedEvent;
        private long myOfflineStartedAt;
        public String myClientAddress;
    }
    
    private class TBroadcast
    {
        public TBroadcast(Object message)
        {
            Message = message;
            SentAt = System.currentTimeMillis();
        }
        public final long SentAt;
        public final Object Message;
    }
    
    public BufferedDuplexInputChannel(IDuplexInputChannel underlyingDuplexInputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannel = underlyingDuplexInputChannel;
            myMaxOfflineTime = maxOfflineTime;

            myMaxOfflineChecker = new Timer("Eneter.MaxOfflineTimer",true);
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
    public Event<ResponseReceiverEventArgs> responseReceiverOnline()
    {
        return myResponseReceiverOnlineEventImpl.getApi();
    }
    
    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverOffline()
    {
        return myResponseReceiverOfflineEventImpl.getApi();
    } 
    

    @Override
    public String getChannelId()
    {
        return myInputChannel.getChannelId();
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myInputChannel.getDispatcher();
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
                myInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
                myInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
                myInputChannel.messageReceived().subscribe(myOnMessageReceived);

                try
                {
                    myInputChannel.startListening();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToStartListening, err);
                    stopListening();
                    throw err;
                }

                myMaxOfflineCheckerRequestedToStop = false;
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
                // Indicate, that the timer responsible for checking if response receivers are timeouted (i.e. exceeded the max offline time)
                // shall stop.
                myMaxOfflineCheckerRequestedToStop = true;
                
                try
                {
                    myInputChannel.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.IncorrectlyStoppedListening, err);
                }

                myResponseReceiversLock.lock();
                try
                {
                    myBroadcasts.clear();
                    myResponseReceivers.clear();
                }
                finally
                {
                    myResponseReceiversLock.unlock();
                }

                myInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
                myInputChannel.responseReceiverDisconnected().unsubscribe(myOnResponseReceiverDisconnected);
                myInputChannel.messageReceived().unsubscribe(myOnMessageReceived);
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
                return myInputChannel.isListening();
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
    public void sendResponseMessage(final String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
         // If it is a broadcast response message.
            if (responseReceiverId.equals("*"))
            {
                myResponseReceiversLock.lock();
                try
                {
                    TBroadcast aBroadcastMessage = new TBroadcast(message);
                    myBroadcasts.add(aBroadcastMessage);

                    for (TBufferedResponseReceiver aResponseReceiver : myResponseReceivers)
                    {
                        // Note: it does not throw exception.
                        aResponseReceiver.sendResponseMessage(message);
                    }
                }
                finally
                {
                    myResponseReceiversLock.unlock();
                }
            }
            else
            {
                boolean aNotifyOffline = false;
                myResponseReceiversLock.lock();
                try
                {
                    TBufferedResponseReceiver aResponseReciever = getResponseReceiver(responseReceiverId);
                    if (aResponseReciever == null)
                    {
                        aResponseReciever = createResponseReceiver(responseReceiverId, "", true);
                        aNotifyOffline = true;
                    }

                    aResponseReciever.sendResponseMessage(message);

                    if (aNotifyOffline)
                    {
                        getDispatcher().invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ResponseReceiverEventArgs anEvent = new ResponseReceiverEventArgs(responseReceiverId, "");
                                notifyEvent(myResponseReceiverOfflineEventImpl, anEvent, false);
                            }
                        });
                    }
                }
                finally
                {
                    myResponseReceiversLock.unlock();
                }
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
            myResponseReceiversLock.lock();
            try
            {
                try
                {
                    HashSetExt.removeWhere(myResponseReceivers, new IFunction1<Boolean, TBufferedResponseReceiver>()
                    {
                        @Override
                        public Boolean invoke(TBufferedResponseReceiver x)
                                throws Exception
                        {
                            return x.ResponseReceiverId.equals(responseReceiverId);
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed in removeWhere to remove a response receiver.");
                }
            }
            finally
            {
                myResponseReceiversLock.unlock();
            }

            myInputChannel.disconnectResponseReceiver(responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aPendingResponseReceicerConnectedEvent = false;
            boolean aNewResponseReceiverFlag = false;
            TBufferedResponseReceiver aResponseReciever;
            myResponseReceiversLock.lock();
            try
            {
                aResponseReciever = getResponseReceiver(e.getResponseReceiverId());
                if (aResponseReciever == null)
                {
                    aResponseReciever = createResponseReceiver(e.getResponseReceiverId(), e.getSenderAddress(), false);
                    aNewResponseReceiverFlag = true;
                }
                
                aResponseReciever.setOnline(true);

                if (aResponseReciever.getPendingResponseReceiverConnectedEvent())
                {
                    aResponseReciever.setClientAddress(e.getSenderAddress());
                    aPendingResponseReceicerConnectedEvent = aResponseReciever.getPendingResponseReceiverConnectedEvent();
                    aResponseReciever.setPendingResponseReceiverConnectedEvent(false);
                }

                if (aNewResponseReceiverFlag)
                {
                    // This is a fresh new response receiver. Therefore broadcast messages were not sent to it yet.
                    for (TBroadcast aBroadcastMessage : myBroadcasts)
                    {
                        aResponseReciever.sendResponseMessage(aBroadcastMessage.Message);
                    }
                }

                // Send all buffered messages.
                aResponseReciever.sendMessagesFromQueue();
            }
            finally
            {
                myResponseReceiversLock.unlock();
            }


            notifyEvent(myResponseReceiverOnlineEventImpl, e, false);
            if (aNewResponseReceiverFlag || aPendingResponseReceicerConnectedEvent)
            {
                notifyEvent(myResponseReceiverConnectedEventImpl, e, false);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aNotify = false;
            myResponseReceiversLock.lock();
            try
            {
                TBufferedResponseReceiver aResponseReciever = getResponseReceiver(e.getResponseReceiverId());
                if (aResponseReciever != null)
                {
                    aResponseReciever.setOnline(false);
                    aNotify = true;
                }
            }
            finally
            {
                myResponseReceiversLock.unlock();
            }

            if (aNotify)
            {
                notifyEvent(myResponseReceiverOfflineEventImpl, e, false);
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
            notifyEvent(myMessageReceivedEventImpl, e, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private TBufferedResponseReceiver getResponseReceiver(final String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TBufferedResponseReceiver aResponseReceiver = null;
            try
            {
                aResponseReceiver = EnumerableExt.firstOrDefault(myResponseReceivers,
                        new IFunction1<Boolean, TBufferedResponseReceiver>()
                        {
                            @Override
                            public Boolean invoke(TBufferedResponseReceiver x)
                            {
                                return x.ResponseReceiverId.equals(responseReceiverId);
                            }
                        });
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed in firstOrDefault when searching response receiver.", err);
            }
                    
            return aResponseReceiver;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private TBufferedResponseReceiver createResponseReceiver(String responseReceiverId, String clientAddress, boolean notifyWhenConnectedFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TBufferedResponseReceiver aResponseReceiver = new TBufferedResponseReceiver(responseReceiverId, myInputChannel);
            
            // Note: if it is created as offline then when the client connects raise ResponseReceiverConnected event.
            aResponseReceiver.setPendingResponseReceiverConnectedEvent(notifyWhenConnectedFlag);

            myResponseReceivers.add(aResponseReceiver);

            // If it is the first response receiver, then start the timer checking which response receivers
            // are disconnected due to the timeout (i.e. max offline time)
            if (myResponseReceivers.size() == 1)
            {
                myMaxOfflineChecker.schedule(getTimerTask(), 300);
            }

            return aResponseReceiver;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMaxOfflineTimeCheckTick() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Do nothing if there is a request to stop.
            if (myMaxOfflineCheckerRequestedToStop)
            {
                return;
            }

            final ArrayList<TBufferedResponseReceiver> aTimeoutedResponseReceivers = new ArrayList<TBufferedResponseReceiver>();

            final long aCurrentCheckTime = System.currentTimeMillis();
            boolean aTimerShallContinueFlag;

            myResponseReceiversLock.lock();
            try
            {
                // Remove all expired broadcasts.
                ArrayListExt.removeAll(myBroadcasts, new IFunction1<Boolean, TBroadcast>()
                {
                    @Override
                    public Boolean invoke(TBroadcast x) throws Exception
                    {
                        return aCurrentCheckTime - x.SentAt > myMaxOfflineTime;
                    }
                });
                
                // Remove all not connected response receivers which exceeded the max offline timeout.
                HashSetExt.removeWhere(myResponseReceivers, new IFunction1<Boolean, TBufferedResponseReceiver>()
                    {
                        @Override
                        public Boolean invoke(TBufferedResponseReceiver x)
                                throws Exception
                        {
                            // If disconnected and max offline time is exceeded. 
                            if (!x.isOnline() &&
                                aCurrentCheckTime - x.getOfflineStartedAt() > myMaxOfflineTime)
                            {
                                aTimeoutedResponseReceivers.add(x);

                                // Indicate, the response receiver can be removed.
                                return true;
                            }

                            // Response receiver will not be removed.
                            return false;
                        }
                    });
                
                aTimerShallContinueFlag = myResponseReceivers.size() > 0;
            }
            finally
            {
                myResponseReceiversLock.unlock();
            }

            // Notify disconnected response receivers.
            for (TBufferedResponseReceiver aResponseReceiverContext : aTimeoutedResponseReceivers)
            {
                // Stop disconnecting if the we are requested to stop.
                if (myMaxOfflineCheckerRequestedToStop)
                {
                    return;
                }
                
                // Invoke the event in the correct thread.
                final ResponseReceiverEventArgs aMsg = new ResponseReceiverEventArgs(aResponseReceiverContext.ResponseReceiverId, aResponseReceiverContext.getClientAddress());
                getDispatcher().invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myResponseReceiverDisconnectedEventImpl, aMsg, false);
                    }
                });
            }

            // If the timer checking the timeout for response receivers shall continue
            if (!myMaxOfflineCheckerRequestedToStop && aTimerShallContinueFlag)
            {
                myMaxOfflineChecker.schedule(getTimerTask(), 300);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private <T> void notifyEvent(EventImpl<T> handler, T event, boolean isNobodySubscribedWarning)
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
    
    /*
     * Helper method to get the new instance of the timer task.
     * The problem is, the timer does not allow to reschedule the same instance of the TimerTask
     * and the exception is thrown.
     */
    private TimerTask getTimerTask()
    {
        TimerTask aTimerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    onMaxOfflineTimeCheckTick();
                }
                catch (Exception e)
                {
                }
            }
        };
        
        return aTimerTask;
    }
    
    
    
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    
    private long myMaxOfflineTime;
    private Timer myMaxOfflineChecker;
    private boolean myMaxOfflineCheckerRequestedToStop;
    private IDuplexInputChannel myInputChannel;

    private ThreadLock myResponseReceiversLock = new ThreadLock();
    private HashSet<TBufferedResponseReceiver> myResponseReceivers = new HashSet<TBufferedResponseReceiver>();
    private ArrayList<TBroadcast> myBroadcasts = new ArrayList<TBroadcast>();
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverOnlineEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverOfflineEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
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
        String aChannelId = (myInputChannel != null) ? myInputChannel.getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }
}
