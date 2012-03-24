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
import eneter.messaging.messagingsystems.composites.ICompositeDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

class BufferedDuplexOutputChannel implements IDuplexOutputChannel, ICompositeDuplexOutputChannel
{
    public BufferedDuplexOutputChannel(IDuplexOutputChannel underlyingDuplexOutputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingDuplexOutputChannel = underlyingDuplexOutputChannel;
            myMaxOfflineTime = maxOfflineTime;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IDuplexOutputChannel getUnderlyingDuplexOutputChannel()
    {
        return myUnderlyingDuplexOutputChannel;
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
        return myUnderlyingDuplexOutputChannel.getChannelId();
    }

    @Override
    public String getResponseReceiverId()
    {
        return myUnderlyingDuplexOutputChannel.getResponseReceiverId();
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

                myUnderlyingDuplexOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
                myUnderlyingDuplexOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);

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
                        catch (Exception e)
                        {
                        }
                    }
                }; 
                        
                ThreadPool.queueUserWorkItem(aDoOpenConnection);

                // Indicate the connection is open.
                myIsConnectedFlag = true;
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
                    mySendingThreadIsStoppedEvent.waitOne(5000);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to wait until the sending thread stop.");
                }

                myIsConnectionOpeningRequestedToStop = true;
                
                try
                {
                    myConnectionOpeningThreadIsStoppedEvent.waitOne(5000);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to wait until the connection opening thread stop.");
                }

                myUnderlyingDuplexOutputChannel.closeConnection();
                myUnderlyingDuplexOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                myUnderlyingDuplexOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);

                // Emty the queue with messages.
                synchronized (myMessagesToSend)
                {
                    myMessagesToSend.clear();
                }


                myIsConnectedFlag = false;
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
                return myIsConnectedFlag;
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
                String aMessage = TracedObject() + ErrorHandler.SendMessageNotConnectedFailure;
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
                            catch (Exception e)
                            {
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

    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseMessageReceivedEventImpl.raise(this, e);
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
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If the opening of the connection is not desired, then just return.
            if (myIsConnectionOpeningRequestedToStop)
            {
                notifyCloseConnection();
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
    
    private void doOpenConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            long aStartConnectionTime = System.currentTimeMillis();

            boolean aConnectionNotEstablished = false;

            // Loop until the connection is open, or the connection openning is requested to stop,
            // or the max offline time expired.
            while (!myIsConnectionOpeningRequestedToStop)
            {
                try
                {
                    myUnderlyingDuplexOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    // The connection failed, so try again.
                    // Or the connection was already open (by some other thread).
                }

                if (myUnderlyingDuplexOutputChannel.isConnected())
                {
                    break;
                }

                // If the max offline time is exceeded, then notify disconnection.
                if (System.currentTimeMillis() - aStartConnectionTime > myMaxOfflineTime)
                {
                    aConnectionNotEstablished = true;
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

            if (!myIsConnectionOpeningRequestedToStop)
            {
                if (aConnectionNotEstablished)
                {
                    closeConnection();
                    notifyCloseConnection();
                }
                else
                {
                    notifyConnectionOpened();
                }
            }
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
                            if (myUnderlyingDuplexOutputChannel.isConnected())
                            {
                                myUnderlyingDuplexOutputChannel.sendMessage(aMessage);

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
    
    private void notifyConnectionOpened()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Runnable aConnectionOpenedInvoker = new Runnable()
            {
                @Override
                public void run()
                {
                    EneterTrace aTrace = EneterTrace.entering();
                    try
                    {
                        if (myConnectionOpenedEventImpl.isSubscribed())
                        {
                            try
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                                myConnectionOpenedEventImpl.raise(this, aMsg);
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
            };
            
            // Invoke the event in a different thread.
            ThreadPool.queueUserWorkItem(aConnectionOpenedInvoker);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyCloseConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myConnectionClosedEventImpl.isSubscribed())
            {
                try
                {
                    DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                    myConnectionClosedEventImpl.raise(this, aMsg);
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
    
    
    private IDuplexOutputChannel myUnderlyingDuplexOutputChannel;
    
    private long myMaxOfflineTime;
    
    private boolean myIsConnectedFlag;
    
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
        String aChannelId = (myUnderlyingDuplexOutputChannel != null) ? myUnderlyingDuplexOutputChannel.getChannelId() : "";
        return "Buffered duplex output channel '" + aChannelId + "' ";
    }
}
