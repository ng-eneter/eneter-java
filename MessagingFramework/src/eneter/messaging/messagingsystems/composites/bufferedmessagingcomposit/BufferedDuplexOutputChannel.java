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
import eneter.net.system.threading.internal.*;

class BufferedDuplexOutputChannel implements IBufferedDuplexOutputChannel
{
    public BufferedDuplexOutputChannel(IDuplexOutputChannel underlyingDuplexOutputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputChannel = underlyingDuplexOutputChannel;
            myMaxOfflineTime = maxOfflineTime;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventImpl.getApi();
    }
    
    @Override
    public Event<DuplexChannelEventArgs> connectionOnline()
    {
        return myConnectionOnlineEventImpl.getApi();
    }
    
    @Override
    public Event<DuplexChannelEventArgs> connectionOffline()
    {
        return myConnectionOfflineEventImpl.getApi();
    }
    

    @Override
    public String getChannelId()
    {
        return myOutputChannel.getChannelId();
    }

    @Override
    public String getResponseReceiverId()
    {
        return myOutputChannel.getResponseReceiverId();
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myOutputChannel.getDispatcher();
    }

    @Override
    public boolean isConnected()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                return myConnectionIsOpenFlag;
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public boolean isOnline()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myOutputChannel.isConnected();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void openConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                if (isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
                myOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
                myOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);

                myConnectionOpeningRequestedToStopFlag = false;
                myConnectionOpeningEndedEvent.reset();

                // Try open connection in a different thread.
                myConnectionOpeningActiveFlag = true;
                Runnable aDoOpenConnection = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        doOpenConnection();
                    }
                }; 
                ThreadPool.queueUserWorkItem(aDoOpenConnection);

                // Indicate the ConnectionOpened evnt shall be raised when the connection is really open.
                myIsConnectionOpenEventPendingFlag = true;

                // Indicate the connection is open.
                myConnectionIsOpenFlag = true;
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
            
            getDispatcher().invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    DuplexChannelEventArgs anEvent = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), "");
                    notifyEvent(myConnectionOfflineEventImpl, anEvent, false);
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                myConnectionOpeningRequestedToStopFlag = true;
                try
                {
                    if (!myConnectionOpeningEndedEvent.waitOne(5000))
                    {
                        EneterTrace.warning(TracedObject() + "failed to stop the connection openning thread within 5 seconds.");
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to wait until the connection opening thread stop.");
                }

                myOutputChannel.closeConnection();
                myOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
                myOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                myOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);

                myMessageQueue.clear();

                myConnectionIsOpenFlag = false;
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                if (!isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.FailedToSendMessageBecauseNotConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                myMessageQueue.add(message);
                sendMessagesFromQueue();
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyEvent(myConnectionOnlineEventImpl, e, false);

            if (myIsConnectionOpenEventPendingFlag)
            {
                notifyEvent(myConnectionOpenedEventImpl, e, false);
            }
            
            myConnectionManipulatorLock.lock();
            try
            {
                sendMessagesFromQueue();
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                // Try to reopen the connection in a different thread.
                if (!myConnectionOpeningActiveFlag)
                {
                    myConnectionOpeningActiveFlag = true;

                    // Start openning in another thread.
                    ThreadPool.queueUserWorkItem(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            doOpenConnection();
                        }
                    });
                }
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
            
            notifyEvent(myConnectionOfflineEventImpl, e, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyEvent(myResponseMessageReceivedEventImpl, e, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void sendMessagesFromQueue()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (isConnected())
            {
                while (myMessageQueue.size() > 0)
                {
                    Object aMessage = myMessageQueue.peek();

                    try
                    {
                        myOutputChannel.sendMessage(aMessage);

                        // Message was successfully sent therefore it can be removed from the queue.
                        myMessageQueue.poll();
                    }
                    catch (Exception err)
                    {
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
    
    private void doOpenConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aConnectionOpenFlag = false;
            long aStartConnectionTime = System.currentTimeMillis();

            // Loop until the connection is open, or the connection opening is requested to stop,
            // or the max offline time expired.
            while (!myConnectionOpeningRequestedToStopFlag)
            {
                try
                {
                    myOutputChannel.openConnection();
                    aConnectionOpenFlag = true;
                    break;
                }
                catch (Exception err)
                {
                    // The connection failed, so try again.
                }

                // If the max offline time is exceeded, then notify disconnection.
                if (System.currentTimeMillis() - aStartConnectionTime > myMaxOfflineTime)
                {
                    break;
                }

                // Do not wait for the next attempt, if the connection opening shall stop.
                if (!myConnectionOpeningRequestedToStopFlag)
                {
                    try
                    {
                        Thread.sleep(300);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }


            // Indicate this connection opening is not active.
            myConnectionOpeningActiveFlag = false;
            myConnectionOpeningEndedEvent.set();
            
            // If opening failed and the connection was meanwhile not explicitly closed.
            if (!myConnectionOpeningRequestedToStopFlag)
            {
                if (!aConnectionOpenFlag)
                {
                    getDispatcher().invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            DuplexChannelEventArgs anEvent = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), "");
                            notifyEvent(myConnectionClosedEventImpl, anEvent, false);
                        }
                    });
                }
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
    
    
    private long myMaxOfflineTime;
    private IDuplexOutputChannel myOutputChannel;
    
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    private boolean myIsConnectionOpenEventPendingFlag;
    private boolean myConnectionIsOpenFlag;
    private boolean myConnectionOpeningActiveFlag;
    private boolean myConnectionOpeningRequestedToStopFlag;
    private ManualResetEvent myConnectionOpeningEndedEvent = new ManualResetEvent(true);
    private ArrayDeque<Object> myMessageQueue = new ArrayDeque<Object>();
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOnlineEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOfflineEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object x, DuplexChannelMessageEventArgs y)
        {
            onResponseMessageReceived(x, y);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpened = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object x, DuplexChannelEventArgs y)
        {
            onConnectionOpened(x, y);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object x, DuplexChannelEventArgs y)
        {
            onConnectionClosed(x, y);
        }
    };
    
    private String TracedObject()
    {
        String aChannelId = (myOutputChannel != null) ? myOutputChannel.getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }

    
}
