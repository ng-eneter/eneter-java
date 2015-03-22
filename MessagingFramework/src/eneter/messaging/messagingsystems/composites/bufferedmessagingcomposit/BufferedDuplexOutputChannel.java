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
import eneter.net.system.threading.internal.*;

class BufferedDuplexOutputChannel implements IDuplexOutputChannel
{
    public BufferedDuplexOutputChannel(IDuplexOutputChannel underlyingDuplexOutputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingOutputChannel = underlyingDuplexOutputChannel;
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
    public String getChannelId()
    {
        return myUnderlyingOutputChannel.getChannelId();
    }

    @Override
    public String getResponseReceiverId()
    {
        return myUnderlyingOutputChannel.getResponseReceiverId();
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myUnderlyingOutputChannel.getDispatcher();
    }

    @Override
    public void openConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myUnderlyingOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
                myUnderlyingOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
                myUnderlyingOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);

                myIsSendingThreadRequestedToStop = false;

                myIsConnectionOpeningRequestedToStop = false;
                myConnectionOpeningThreadIsStoppedEvent.reset();

                // Try open connection in a different thread.
                myIsConnectionOpeningActive = true;
                Runnable aDoOpenConnection = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            doOpenConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                }; 
                        
                ThreadPool.queueUserWorkItem(aDoOpenConnection);

                // Indicate the connection is open.
                myIsOpenConnectionCalledFlag = true;
            }
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
            synchronized (myConnectionManipulatorLock)
            {
                myIsSendingThreadRequestedToStop = true;
                
                try
                {
                    if (!mySendingThreadIsStoppedEvent.waitOne(5000))
                    {
                        EneterTrace.warning(TracedObject() + "failed to stop the message sending thread within 5 seconds.");
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to wait until the sending thread stop.");
                }

                myIsConnectionOpeningRequestedToStop = true;
                
                try
                {
                    if (!myConnectionOpeningThreadIsStoppedEvent.waitOne(5000))
                    {
                        EneterTrace.warning(TracedObject() + "failed to stop the connection openning thread within 5 seconds.");
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to wait until the connection opening thread stop.");
                }

                myUnderlyingOutputChannel.closeConnection();
                myUnderlyingOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
                myUnderlyingOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                myUnderlyingOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);

                // Emty the queue with messages.
                synchronized (myMessagesToSend)
                {
                    myMessagesToSend.clear();
                }


                myIsOpenConnectionCalledFlag = false;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                return myIsOpenConnectionCalledFlag;
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
            if (!isConnected())
            {
                String aMessage = TracedObject() + ErrorHandler.FailedToSendMessageBecauseNotConnected;
                EneterTrace.error(aMessage);
                throw new IllegalStateException(aMessage);
            }

            synchronized (myMessagesToSend)
            {
                myMessagesToSend.add(message);

                if (!mySendingThreadActiveFlag)
                {
                    mySendingThreadActiveFlag = true;
                    mySendingThreadIsStoppedEvent.reset();

                    // Start thread responsible for sending messages.
                    Runnable aSender = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                doMessageSending();
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                    }; 

                    ThreadPool.queueUserWorkItem(aSender);
                }
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
            notifyEvent(myConnectionOpenedEventImpl, e, false);
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
            // If the opening of the connection is not desired, then just return.
            if (myIsConnectionOpeningRequestedToStop)
            {
                notifyEvent(myConnectionClosedEventImpl, e, false);
                return;
            }

            // Try to reopen the connection in a different thread.
            if (!myIsConnectionOpeningActive)
            {
                myIsConnectionOpeningActive = true;

                // Start openning in another thread.
                Runnable aDoOpenConnection = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            doOpenConnection();
                        }
                        catch (Exception e)
                        {
                        }
                    }
                }; 
                ThreadPool.queueUserWorkItem(aDoOpenConnection);
            }
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
    
    private void doOpenConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            long aStartConnectionTime = System.currentTimeMillis();

            // Loop until the connection is open, or the connection openning is requested to stop,
            // or the max offline time expired.
            while (!myIsConnectionOpeningRequestedToStop)
            {
                try
                {
                    myUnderlyingOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    // The connection failed, so try again.
                    // Or the connection was already open (by some other thread).
                }

                if (myUnderlyingOutputChannel.isConnected())
                {
                    break;
                }

                // If the max offline time is exceeded, then notify disconnection.
                if (System.currentTimeMillis() - aStartConnectionTime > myMaxOfflineTime)
                {
                    break;
                }

                // Do not wait for the next attempt, if the connection opening shall stop.
                if (!myIsConnectionOpeningRequestedToStop)
                {
                    Thread.sleep(300);
                }
            }


            // Indicate this connection opening is not active.
            // The CloseConnection() is going to be called.
            // There is the WaitOne(), waiting until the
            myIsConnectionOpeningActive = false;
            myConnectionOpeningThreadIsStoppedEvent.set();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void doMessageSending() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Loop taking messages from the queue, until the queue is empty or there is a request to stop sending.
                while (!myIsSendingThreadRequestedToStop)
                {
                    Object aMessage;

                    synchronized (myMessagesToSend)
                    {
                        // If there is a message in the queue, read it.
                        if (myMessagesToSend.size() > 0)
                        {
                            aMessage = myMessagesToSend.get(0);
                        }
                        else
                        {
                            // There are no messages in the queue, therefore the thread can end.
                            return;
                        }
                    }


                    // Loop until the message is sent or until there is no request to stop sending.
                    while (!myIsSendingThreadRequestedToStop)
                    {
                        try
                        {
                            if (myUnderlyingOutputChannel.isConnected())
                            {
                                myUnderlyingOutputChannel.sendMessage(aMessage);

                                // The message was successfuly sent, therefore remove it from the queue.
                                synchronized (myMessagesToSend)
                                {
                                    myMessagesToSend.remove(0);
                                }

                                break;
                            }
                        }
                        catch (Exception err)
                        {
                            // The sending of the message failed, therefore wait for a while and try again.
                        }

                        // Do not wait if there is a request to stop the sending.
                        if (!myIsSendingThreadRequestedToStop)
                        {
                            Thread.sleep(300);
                        }
                    }
                }
            }
            finally
            {
                mySendingThreadActiveFlag = false;
                mySendingThreadIsStoppedEvent.set();
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
    private IDuplexOutputChannel myUnderlyingOutputChannel;
    
    private boolean myIsOpenConnectionCalledFlag;
    
    private boolean mySendingThreadActiveFlag;
    private boolean myIsSendingThreadRequestedToStop;
    private ManualResetEvent mySendingThreadIsStoppedEvent = new ManualResetEvent(true);

    private boolean myIsConnectionOpeningActive;
    private boolean myIsConnectionOpeningRequestedToStop;
    private ManualResetEvent myConnectionOpeningThreadIsStoppedEvent = new ManualResetEvent(true);

    private Object myConnectionManipulatorLock = new Object();
    private ArrayList<Object> myMessagesToSend = new ArrayList<Object>();
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
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
        String aChannelId = (myUnderlyingOutputChannel != null) ? myUnderlyingOutputChannel.getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }

    
}
