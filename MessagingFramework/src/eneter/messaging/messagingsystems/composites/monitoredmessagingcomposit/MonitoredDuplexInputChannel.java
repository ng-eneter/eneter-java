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
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.linq.internal.EnumerableExt;


class MonitoredDuplexInputChannel implements IDuplexInputChannel
{
    private class TResponseReceiverContext
    {
        public TResponseReceiverContext(String responseReceiverId, String clientAddress, long lastUpdateTime)
        {
            myResponseReceiverId = responseReceiverId;
            myLastUpdateTime = lastUpdateTime;
            myClientAddress = clientAddress;
        }

        public String getResponseReceiverId()
        {
            return myResponseReceiverId;
        }
        
        public long getLastUpdateTime()
        {
            return myLastUpdateTime;
        }
        
        public String getClientAddress()
        {
            return myClientAddress;
        }
        
        private String myResponseReceiverId;
        private long myLastUpdateTime;
        private String myClientAddress;
    }
    
    
    public MonitoredDuplexInputChannel(IDuplexInputChannel underlyingInputChannel, ISerializer serializer, long pingTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingInputChannel = underlyingInputChannel;
            mySerializer = serializer;
            myPingTimeout = pingTimeout;

            // Create timer checking if pings are received within desired time-window.
            // Note: The timer thread is daemon.
            myPingTimeoutChecker = new Timer(true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public Event<ConnectionTokenEventArgs> responseReceiverConnecting()
    {
        return myResponseReceiverConnectingEventImpl.getApi();
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
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myUnderlyingInputChannel.responseReceiverConnecting().subscribe(myOnResponseReceiverConecting);
                myUnderlyingInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
                myUnderlyingInputChannel.messageReceived().subscribe(myOnMessageReceived);

                try
                {
                    myUnderlyingInputChannel.startListening();
                }
                catch (Exception err)
                {
                    myUnderlyingInputChannel.responseReceiverConnecting().unsubscribe(myOnResponseReceiverConecting);
                    myUnderlyingInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
                    myUnderlyingInputChannel.messageReceived().unsubscribe(myOnMessageReceived);

                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                }
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
                try
                {
                    myUnderlyingInputChannel.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }

                myUnderlyingInputChannel.responseReceiverConnecting().unsubscribe(myOnResponseReceiverConecting);
                myUnderlyingInputChannel.responseReceiverConnected().unsubscribe(myOnResponseReceiverConnected);
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
                return myUnderlyingInputChannel != null && myUnderlyingInputChannel.isListening();
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
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myUnderlyingInputChannel.disconnectResponseReceiver(responseReceiverId);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DisconnectResponseReceiverFailure + responseReceiverId, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverConecting(Object sender, ConnectionTokenEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectingEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectingEventImpl.raise(this, e);
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

    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Update the activity time for the response receiver
            updateResponseReceiver(e.getResponseReceiverId(), e.getSenderAddress());

            if (myResponseReceiverConnectedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectedEventImpl.raise(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
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
    
    private void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Update the activity time for the response receiver
                updateResponseReceiver(e.getResponseReceiverId(), e.getSenderAddress());

                // Deserialize the incoming message.
                MonitorChannelMessage aMessage = mySerializer.deserialize(e.getMessage(), MonitorChannelMessage.class);

                // if the message is ping, then response.
                if (aMessage.MessageType == MonitorChannelMessageType.Ping)
                {
                    try
                    {
                        myUnderlyingInputChannel.sendResponseMessage(e.getResponseReceiverId(), e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to response the ping message.", err);
                    }
                }
                else
                {
                    // Notify the incoming message.
                    if (myMessageReceivedEventImpl.isSubscribed())
                    {
                        DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.MessageContent, e.getResponseReceiverId(), e.getSenderAddress());

                        try
                        {
                            myMessageReceivedEventImpl.raise(this, aMsg);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageFailure, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onPingTimeoutCheckerTick()
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<TResponseReceiverContext> aTimeoutedResponseReceivers = new ArrayList<TResponseReceiverContext>();

            boolean aContinueTimerFlag = false;

            synchronized (myResponseReceiverContexts)
            {
                final long aCurrentTime = System.currentTimeMillis();

                HashSetExt.removeWhere(myResponseReceiverContexts,
                        new IFunction1<Boolean, TResponseReceiverContext>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiverContext x)
                                    throws Exception
                            {
                                if (aCurrentTime - x.getLastUpdateTime() > myPingTimeout)
                                {
                                    // Store the timeouted response receiver.
                                    aTimeoutedResponseReceivers.add(x);

                                    // Indicate, that the response receiver can be removed.
                                    return true;
                                }

                                // Indicate, that the response receiver cannot be removed.
                                return false;
                            }
                        });

                aContinueTimerFlag = myResponseReceiverContexts.size() > 0;
            }

            // Close connection for all timeouted response receivers.
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
                myUnderlyingInputChannel.getDispatcher().invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyResponseReceiverDisconnected(e);
                    }
                });
            }

            // If the timer chall continue.
            if (aContinueTimerFlag)
            {
                myPingTimeoutChecker.schedule(getTimerTask(), 500);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void updateResponseReceiver(final String responseReceiverId, String clientAddress)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseReceiverContexts)
            {
                boolean aStartTimerFlag = myResponseReceiverContexts.size() == 0;

                long aCurrentTime = System.currentTimeMillis();

                TResponseReceiverContext aResponseReceiverContext = EnumerableExt.firstOrDefault(myResponseReceiverContexts,
                        new IFunction1<Boolean, TResponseReceiverContext>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiverContext x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                        });

                if (aResponseReceiverContext == null)
                {
                    aResponseReceiverContext = new TResponseReceiverContext(responseReceiverId, clientAddress, aCurrentTime);
                    myResponseReceiverContexts.add(aResponseReceiverContext);
                }
                else
                {
                    aResponseReceiverContext.myLastUpdateTime = aCurrentTime;
                }

                if (aStartTimerFlag)
                {
                    myPingTimeoutChecker.schedule(getTimerTask(), 500);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyResponseReceiverDisconnected(ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverDisconnectedEventImpl.raise(this, e);
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
                    onPingTimeoutCheckerTick();
                }
                catch (Exception e)
                {
                }
            }
        };
        
        return aTimerTask;
    }
    
    
    private Object myListeningManipulatorLock = new Object();
    private IDuplexInputChannel myUnderlyingInputChannel;
    private ISerializer mySerializer;

    private long myPingTimeout;
    private Timer myPingTimeoutChecker;
    private HashSet<TResponseReceiverContext> myResponseReceiverContexts = new HashSet<TResponseReceiverContext>();
    
    
    private EventImpl<ConnectionTokenEventArgs> myResponseReceiverConnectingEventImpl = new EventImpl<ConnectionTokenEventArgs>();
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private EventHandler<ConnectionTokenEventArgs> myOnResponseReceiverConecting = new EventHandler<ConnectionTokenEventArgs>()
    {
        @Override
        public void onEvent(Object x, ConnectionTokenEventArgs y)
        {
            onResponseReceiverConecting(x, y);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object x, ResponseReceiverEventArgs y)
        {
            onResponseReceiverConnected(x, y);
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
