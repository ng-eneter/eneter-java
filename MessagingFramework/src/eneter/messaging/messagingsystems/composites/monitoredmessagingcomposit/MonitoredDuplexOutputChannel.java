/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.threading.internal.EneterTimer;


class MonitoredDuplexOutputChannel implements IDuplexOutputChannel
{
    public MonitoredDuplexOutputChannel(IDuplexOutputChannel underlyingOutputChannel, ISerializer serializer,
            long pingFrequency,
            long receiveTimeout,
            IThreadDispatcher dispatcher)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingOutputChannel = underlyingOutputChannel;

            mySerializer = serializer;
            myPingFrequency = pingFrequency;
            myReceiveTimeout = receiveTimeout;
            myDispatcher = dispatcher;
            
            MonitorChannelMessage aPingMessage = new MonitorChannelMessage(MonitorChannelMessageType.Ping, null);
            myPreserializedPingMessage = mySerializer.serialize(aPingMessage, MonitorChannelMessage.class);
            
            myPingingTimer = new EneterTimer(new Runnable()
            {
                @Override
                public void run()
                {
                    onPingingTimerTick();
                }
            }, "Eneter.ClientPingTimer");
            myReceiveTimer = new EneterTimer(new Runnable()
            {
                @Override
                public void run()
                {
                    onResponseTimerTick();
                }
            }, "Eneter.ClientMonitorReceiveTimer");
            
            myUnderlyingOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
            myUnderlyingOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
            myUnderlyingOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
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
        return myDispatcher;
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

                try
                {
                    // Start timers.
                    myPingingTimer.change(myPingFrequency);
                    myReceiveTimer.change(myReceiveTimeout);

                    // Open connection in the underlying channel.
                    myUnderlyingOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToOpenConnection, err);
                    closeConnection();
                    throw err;
                }
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
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            cleanAfterConnection(true, false);
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
            myConnectionManipulatorLock.lock();
            try
            {
                return myUnderlyingOutputChannel.isConnected();
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
                    String anError = TracedObject() + ErrorHandler.FailedToSendMessageBecauseNotConnected;
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                try
                {
                    // Get the message recognized by the monitor duplex input channel.
                    MonitorChannelMessage aMessage = new MonitorChannelMessage(MonitorChannelMessageType.Message, message);
                    Object aSerializedMessage = mySerializer.serialize(aMessage, MonitorChannelMessage.class);
    
                    // Send the message by using the underlying messaging system.
                    myUnderlyingOutputChannel.sendMessage(aSerializedMessage);
                    
                    // Reschedule the ping.
                    myPingingTimer.change(myPingFrequency);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + ErrorHandler.FailedToSendMessage;
                    EneterTrace.error(anErrorMessage, err);
                    
                    cleanAfterConnection(true, true);
                    
                    throw err;
                }
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
    
    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Deserialize the message.
                MonitorChannelMessage aMessage = mySerializer.deserialize(e.getMessage(), MonitorChannelMessage.class);

                // Note: timer setting is after deserialization.
                //       reason: if deserialization fails the timer is not updated and the client will be disconnected.
                myConnectionManipulatorLock.lock();
                try
                {
                    // Cancel the current response timeout and set the new one.
                    myReceiveTimer.change(myReceiveTimeout);
                }
                finally
                {
                    myConnectionManipulatorLock.unlock();
                }
                
                // If it is a message.
                if (aMessage.MessageType == MonitorChannelMessageType.Message)
                {
                    final DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.MessageContent, e.getResponseReceiverId(), e.getSenderAddress());
                    myDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            notifyEvent(myResponseMessageReceivedEventImpl, aMsg, true);
                        }
                    }); 
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
    
    private void onConnectionOpened(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEvent(myConnectionOpenedEventImpl, e, false);
                }
            });
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
            cleanAfterConnection(false, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // The method is called if the inactivity (not sending messages) exceeded the pinging frequency time.
    private void onPingingTimerTick()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myConnectionManipulatorLock.lock();
                try
                {
                    // Send the ping message.
                    myUnderlyingOutputChannel.sendMessage(myPreserializedPingMessage);

                    // Schedule the next ping.
                    myPingingTimer.change(myPingFrequency);
                }
                finally
                {
                    myConnectionManipulatorLock.unlock();
                }
            }
            catch (Exception err)
            {
                // The sending of the ping message failed - the connection is broken.
                cleanAfterConnection(true, true);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // The method is called if there is no message from the input channel within response timeout.
    private void onResponseTimerTick()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            cleanAfterConnection(true, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void cleanAfterConnection(boolean sendCloseMessageFlag, boolean notifyConnectionClosedFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                // Stop timers.
                try
                {
                    myPingingTimer.change(-1);
                }
                catch (Exception err)
                {
                    // n.a.
                }
                try
                {
                    myReceiveTimer.change(-1);
                }
                catch (Exception err)
                {
                    // n.a.
                }
                
                if (sendCloseMessageFlag)
                {
                    myUnderlyingOutputChannel.closeConnection();
                }
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }

            if (notifyConnectionClosedFlag)
            {
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myConnectionClosedEventImpl, new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), ""), false);
                    }
                });
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
    
    
    private IDuplexOutputChannel myUnderlyingOutputChannel;
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    
    private EneterTimer myPingingTimer;
    private long myPingFrequency;
    private IThreadDispatcher myDispatcher;
    
    private EneterTimer myReceiveTimer;
    private long myReceiveTimeout;
    
    private ISerializer mySerializer;
    private Object myPreserializedPingMessage;

    
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
