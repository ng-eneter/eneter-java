/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.lang.Thread.State;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.threading.internal.*;

class MonitoredDuplexOutputChannel implements IDuplexOutputChannel
{
    public MonitoredDuplexOutputChannel(IDuplexOutputChannel underlyingOutputChannel, ISerializer serializer, long pingFrequency, long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingOutputChannel = underlyingOutputChannel;

            mySerializer = serializer;
            myPingFrequency = pingFrequency;
            myPingResponseTimeout = pingResponseTimeout;
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

                myUnderlyingOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
                myUnderlyingOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);

                try
                {
                    // Open connection in the underlying channel.
                    myUnderlyingOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    myUnderlyingOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                    myUnderlyingOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);

                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToOpenConnection, err);

                    throw err;
                }

                // Indicate, the pinging shall run.
                myPingingRequestedToStopFlag = false;
                myPingFrequencyWaiting.reset();
                myResponseReceivedEvent.reset();

                myPingingThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            doPinging();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                });
                myPingingThread.start();
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
                // Indicate, the pinging thread shall stop.
                // Note: when pinging thread stops, it also closes the underlying duplex output channel.
                myPingingRequestedToStopFlag = true;

                // Interrupt waiting for the frequency time.
                myPingFrequencyWaiting.set();

                // Interrupt waiting for the response of the ping.
                myResponseReceivedEvent.set();

                // Wait until the pinging thread stopped.
                if (myPingingThread != null && myPingingThread.getState() != State.NEW)
                {
                    try
                    {
                        myPingingThread.join(3000);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of the pinging thread. The thread id = " + myPingingThread.getId());
                    }
                    
                    if (myPingingThread.getState() != State.TERMINATED)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToStopThreadId + myPingingThread.getId());

                        try
                        {
                            myPingingThread.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.FailedToAbortThread, err);
                        }
                    }
                }
                myPingingThread = null;
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
                return myUnderlyingOutputChannel.isConnected();
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
            synchronized (myConnectionManipulatorLock)
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
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + ErrorHandler.FailedToSendMessage;
                    EneterTrace.error(anErrorMessage, err);
                    throw err;
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
            try
            {
                // Deserialize the message.
                MonitorChannelMessage aMessage = mySerializer.deserialize(e.getMessage(), MonitorChannelMessage.class);

                // If it is the response for the ping.
                if (aMessage.MessageType == MonitorChannelMessageType.Ping)
                {
                    // Release the pinging thread waiting for the response.
                    myResponseReceivedEvent.set();
                }
                else
                {
                    // Notify the event to the subscriber.
                    DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.MessageContent, e.getResponseReceiverId(), e.getSenderAddress());
                    notifyEvent(myResponseMessageReceivedEventImpl, aMsg, true);
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
    
    private void doPinging() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // While the flag indicates, the pinging shall run.
            while (!myPingingRequestedToStopFlag)
            {
                try
                {
                    // Wait, before the next ping.
                    myPingFrequencyWaiting.waitOne(myPingFrequency);

                    if (!myPingingRequestedToStopFlag)
                    {
                        // Send the ping message.
                        MonitorChannelMessage aPingMessage = new MonitorChannelMessage(MonitorChannelMessageType.Ping, null);
                        Object aSerializedPingMessage = mySerializer.serialize(aPingMessage, MonitorChannelMessage.class);
                        myUnderlyingOutputChannel.sendMessage(aSerializedPingMessage);
                    }
                }
                catch (Exception err)
                {
                    // The sending of the ping message failed.
                    // Therefore the connection is broken and the pinging will be stopped.
                    break;
                }

                // If the response does not come in defined time, then it consider that as broken connection.
                if (!myResponseReceivedEvent.waitOne(myPingResponseTimeout))
                {
                    break;
                }
            }

            // Close the underlying channel.
            if (myUnderlyingOutputChannel != null)
            {
                try
                {
                    // Close connection in the underlying channel.
                    myUnderlyingOutputChannel.closeConnection();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
                }

                myUnderlyingOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                myUnderlyingOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
            }

            // Notify, the connection is closed.
            final DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), "");
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    myUnderlyingOutputChannel.getDispatcher().invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            notifyEvent(myConnectionClosedEventImpl, aMsg, false);
                        }
                    });
                }
            });
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
    private Object myConnectionManipulatorLock = new Object();
    private long myPingFrequency;
    private AutoResetEvent myPingFrequencyWaiting = new AutoResetEvent(false);
    private long myPingResponseTimeout;
    private volatile boolean myPingingRequestedToStopFlag;
    private AutoResetEvent myResponseReceivedEvent = new AutoResetEvent(false);
    private Thread myPingingThread;

    private ISerializer mySerializer;

    
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
    
    
    private String TracedObject()
    {
        String aChannelId = (myUnderlyingOutputChannel != null) ? myUnderlyingOutputChannel.getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }

}
