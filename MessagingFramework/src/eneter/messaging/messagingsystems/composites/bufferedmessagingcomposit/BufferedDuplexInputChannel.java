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
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.internal.StringExt;
import eneter.net.system.linq.internal.EnumerableExt;

class BufferedDuplexInputChannel implements IDuplexInputChannel
{
    public BufferedDuplexInputChannel(IDuplexInputChannel underlyingDuplexInputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingInputChannel = underlyingDuplexInputChannel;
            myMaxOfflineTime = maxOfflineTime;

            myMaxOfflineChecker = new Timer("MaxOfflineChecker",true);
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
        return myUnderlyingInputChannel.getDispatcher();
    }
    
    @Override
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                myUnderlyingInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
                myUnderlyingInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
                myUnderlyingInputChannel.messageReceived().subscribe(myOnMessageReceived);

                try
                {
                    myUnderlyingInputChannel.startListening();
                }
                catch (Exception err)
                {
                    myUnderlyingInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
                    myUnderlyingInputChannel.responseReceiverDisconnected().unsubscribe(myOnResponseReceiverDisconnected);
                    myUnderlyingInputChannel.messageReceived().unsubscribe(myOnMessageReceived);

                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                }

                myMaxOfflineCheckerRequestedToStop = false;
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
                // Indicate, that the timer responsible for checking if response receivers are timeouted (i.e. exceeded the max offline time)
                // shall stop.
                myMaxOfflineCheckerRequestedToStop = true;

                synchronized (myResponseReceivers)
                {
                    // Stop buffering for all connected response receivers.
                    for (ResponseReceiverContext aResponseReceiverContext : myResponseReceivers)
                    {
                        aResponseReceiverContext.stopSendingOfResponseMessages();
                    }

                    myResponseReceivers.clear();
                }
                
                try
                {
                    myUnderlyingInputChannel.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }

                myUnderlyingInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
                myUnderlyingInputChannel.responseReceiverDisconnected().unsubscribe(myOnResponseReceiverDisconnected);
                myUnderlyingInputChannel.messageReceived().unsubscribe(myOnMessageReceived);
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
                return myUnderlyingInputChannel.isListening();
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
            ResponseReceiverContext aResponseReceiverContext;

            synchronized (myResponseReceivers)
            {
                aResponseReceiverContext = EnumerableExt.firstOrDefault(myResponseReceivers,
                        new IFunction1<Boolean, ResponseReceiverContext>()
                        {
                            @Override
                            public Boolean invoke(ResponseReceiverContext x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                    
                        });
                        

                // If the response receiver was not found, then it means, that the response receiver is not connected.
                // In order to support independent startup order, we can suppose, the response receiver can connect later
                // and the response messages will be then delivered.
                // If not, then response messages will be deleted automatically after the timeout (maxOfflineTime).
                if (aResponseReceiverContext == null)
                {
                    // Create the response receiver context - it allows to enqueue response messages before connection of
                    // the response receiver.
                    aResponseReceiverContext = new ResponseReceiverContext(responseReceiverId, "", myUnderlyingInputChannel);
                    myResponseReceivers.add(aResponseReceiverContext);

                    // If it is the first response receiver, then start the timer checking which response receivers
                    // are disconnected due to the timeout (i.e. max offline time)
                    if (myResponseReceivers.size() == 1)
                    {
                        myMaxOfflineChecker.schedule(getTimerTask(), 300);
                    }

                }
            }

            // Enqueue the message.
            aResponseReceiverContext.sendResponseMessage(message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(final String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseReceivers)
            {
                ResponseReceiverContext aResponseReceiverContext = EnumerableExt.firstOrDefault(myResponseReceivers,
                        new IFunction1<Boolean, ResponseReceiverContext>()
                        {
                            @Override
                            public Boolean invoke(ResponseReceiverContext x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                     
                        });
                        
                if (aResponseReceiverContext != null)
                {
                    // Stop the buffer queue.
                    aResponseReceiverContext.stopSendingOfResponseMessages();

                    // Remove the receiver from the list.
                    myResponseReceivers.remove(aResponseReceiverContext);
                }
            }

            myUnderlyingInputChannel.disconnectResponseReceiver(responseReceiverId);
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
            // Update the time for the response receiver.
            // If the response receiver does not exist, then create it.
            updateResponseReceiverContext(e.getResponseReceiverId(), e.getSenderAddress(), true, true);

            notifyEvent(myResponseReceiverConnectedEventImpl, e, false);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when response receiver connected.", err);
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
            // Update that the response receiver is disconnected.
            updateResponseReceiverContext(e.getResponseReceiverId(), e.getSenderAddress(), false, false);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException);
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
            // Update the time for the response receiver.
            // If the response receiver does not exist, then create it.
            updateResponseReceiverContext(e.getResponseReceiverId(), e.getSenderAddress(), true, true);
            
            notifyEvent(myMessageReceivedEventImpl, e, true);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when message was received.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private void updateResponseReceiverContext(final String responseReceiverId, String clientAddress, boolean isConnected, boolean createNewIfDoesNotExistFlag)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseReceivers)
            {
                ResponseReceiverContext aResponseReceiverContext = EnumerableExt.firstOrDefault(myResponseReceivers,
                    new IFunction1<Boolean, ResponseReceiverContext>()
                    {
                        @Override
                        public Boolean invoke(ResponseReceiverContext x)
                                throws Exception
                        {
                            return x.getResponseReceiverId().equals(responseReceiverId);
                        }
                    });
                

                // If the response receiver was not found, then it means, that the response receiver is not connected.
                // In order to support independent startup order, we can suppose, the response receiver can connect later
                // and the response messages will be then delivered.
                // If not, then response messages will be deleted automatically after the timeout (maxOfflineTime).
                if (aResponseReceiverContext == null && createNewIfDoesNotExistFlag)
                {
                    // Create the response receiver context - it allows to enqueue response messages before connection of
                    // the response receiver.
                    aResponseReceiverContext = new ResponseReceiverContext(responseReceiverId, clientAddress, myUnderlyingInputChannel);
                    aResponseReceiverContext.setConnectionState(isConnected);
                    myResponseReceivers.add(aResponseReceiverContext);

                    // If it is the first response receiver, then start the timer checking which response receivers
                    // are disconnected due to the timeout (i.e. max offline time)
                    if (myResponseReceivers.size() == 1)
                    {
                        myMaxOfflineChecker.schedule(getTimerTask(), 300);
                    }
                }

                // Update the connection status.
                if (aResponseReceiverContext != null)
                {
                    aResponseReceiverContext.setConnectionState(isConnected);

                    if (!StringExt.isNullOrEmpty(clientAddress))
                    {
                        aResponseReceiverContext.setClientAddress(clientAddress);
                    }
                }
            }
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

            final ArrayList<ResponseReceiverContext> aTimeoutedResponseReceivers = new ArrayList<ResponseReceiverContext>();

            final long aCurrentCheckTime = System.currentTimeMillis();
            boolean aTimerShallContinueFlag;

            synchronized (myResponseReceivers)
            {
                HashSetExt.removeWhere(myResponseReceivers, new IFunction1<Boolean, ResponseReceiverContext>()
                    {
                        @Override
                        public Boolean invoke(ResponseReceiverContext x)
                                throws Exception
                        {
                            // If disconnected and max offline time is exceeded. 
                            if (!x.isResponseReceiverConnected() &&
                                aCurrentCheckTime - x.getLastConnectionChangeTime() > myMaxOfflineTime)
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

            // Notify disconnected response receivers.
            for (ResponseReceiverContext aResponseReceiverContext : aTimeoutedResponseReceivers)
            {
                // Stop disconnecting if the we are requested to stop.
                if (myMaxOfflineCheckerRequestedToStop)
                {
                    return;
                }
                
                aResponseReceiverContext.stopSendingOfResponseMessages();

                // Try to disconnect the response receiver.
                try
                {
                    myUnderlyingInputChannel.disconnectResponseReceiver(aResponseReceiverContext.getResponseReceiverId());
                }
                catch (Exception err)
                {
                    // The exception could occur because the response receiver is not connected.
                    // It is ok.
                }

                final ResponseReceiverEventArgs aMsg = new ResponseReceiverEventArgs(aResponseReceiverContext.getResponseReceiverId(), aResponseReceiverContext.getClientAddress());
                myUnderlyingInputChannel.getDispatcher().invoke(new Runnable()
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
    
    
    
    private Object myListeningManipulatorLock = new Object();
    
    private long myMaxOfflineTime;
    private Timer myMaxOfflineChecker;
    private boolean myMaxOfflineCheckerRequestedToStop;
    private IDuplexInputChannel myUnderlyingInputChannel;

    private HashSet<ResponseReceiverContext> myResponseReceivers = new HashSet<ResponseReceiverContext>();
    
    
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
