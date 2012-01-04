package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.lang.Thread.State;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.composites.ICompositeDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

class MonitoredDuplexOutputChannel implements IDuplexOutputChannel, ICompositeDuplexOutputChannel
{
    public MonitoredDuplexOutputChannel(IDuplexOutputChannel underlyingOutputChannel, ISerializer serializer, long pingFrequency, long pingResponseTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingDuplexOutputChannel = underlyingOutputChannel;

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
    public IDuplexOutputChannel getUnderlyingDuplexOutputChannel()
    {
        return myUnderlyingDuplexOutputChannel;
    }

    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventApi;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventApi;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventApi;
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

                myUnderlyingDuplexOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
                myUnderlyingDuplexOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
                myUnderlyingDuplexOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);

                try
                {
                    // Open connection in the underlying channel.
                    myUnderlyingDuplexOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    myUnderlyingDuplexOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                    myUnderlyingDuplexOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
                    myUnderlyingDuplexOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);

                    EneterTrace.error(TracedObject() + ErrorHandler.OpenConnectionFailure, err);

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
                        catch (Exception e)
                        {
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
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure + myPingingThread.getId());

                        try
                        {
                            myPingingThread.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                        }
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                            throw err;
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
                return myUnderlyingDuplexOutputChannel.isConnected();
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
                    String anError = TracedObject() + ErrorHandler.SendMessageNotConnectedFailure;
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                try
                {
                    // Get the message recognized by the monitor duplex input channel.
                    MonitorChannelMessage aMessage = new MonitorChannelMessage(MonitorChannelMessageType.Message, message);
                    Object aSerializedMessage = mySerializer.serialize(aMessage, MonitorChannelMessage.class);
    
                    // Send the message by using the underlying messaging system.
                    myUnderlyingDuplexOutputChannel.sendMessage(aSerializedMessage);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + ErrorHandler.SendMessageFailure;
                    EneterTrace.error(anErrorMessage, err);
                    throw err;
                }
                catch (Error err)
                {
                    String anErrorMessage = TracedObject() + ErrorHandler.SendMessageFailure;
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
                if (aMessage.myMessageType == MonitorChannelMessageType.Ping)
                {
                    // Release the pinging thread waiting for the response.
                    myResponseReceivedEvent.set();
                }
                else
                {
                    // Notify the event to the subscriber.
                    if (myResponseMessageReceivedEventImpl.isEmpty() == false)
                    {
                        DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.myMessageContent, e.getResponseReceiverId());

                        try
                        {
                            myResponseMessageReceivedEventImpl.update(this, aMsg);
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
    
    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myConnectionOpenedEventImpl.isEmpty() == false)
            {
                try
                {
                    myConnectionOpenedEventImpl.update(this, e);
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
            closeConnection();
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
                        myUnderlyingDuplexOutputChannel.sendMessage(aSerializedPingMessage);
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
            if (myUnderlyingDuplexOutputChannel != null)
            {
                try
                {
                    // Close connection in the underlying channel.
                    myUnderlyingDuplexOutputChannel.closeConnection();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                }

                myUnderlyingDuplexOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                myUnderlyingDuplexOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
                myUnderlyingDuplexOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
            }

            // Notify, the connection is closed.
            notifyConnectionClosed();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyConnectionClosed()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Runnable aConnectionClosedInvoker = new Runnable()
            {
                @Override
                public void run()
                {
                    EneterTrace aTrace = EneterTrace.entering();
                    try
                    {
                        if (myConnectionClosedEventImpl.isEmpty() == false)
                        {
                            try
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                                myConnectionClosedEventImpl.update(this, aMsg);
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
            ThreadPool.queueUserWorkItem(aConnectionClosedInvoker);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IDuplexOutputChannel myUnderlyingDuplexOutputChannel;
    private Object myConnectionManipulatorLock = new Object();
    private long myPingFrequency;
    private AutoResetEvent myPingFrequencyWaiting = new AutoResetEvent(false);
    private long myPingResponseTimeout;
    private volatile boolean myPingingRequestedToStopFlag;
    private AutoResetEvent myResponseReceivedEvent = new AutoResetEvent(false);
    private Thread myPingingThread;

    private ISerializer mySerializer;

    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private Event<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventApi = new Event<DuplexChannelMessageEventArgs>(myResponseMessageReceivedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionOpenedEventApi = new Event<DuplexChannelEventArgs>(myConnectionOpenedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionClosedEventApi = new Event<DuplexChannelEventArgs>(myConnectionClosedEventImpl);

    
    private IMethod2<Object, DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new IMethod2<Object, DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object x, DuplexChannelMessageEventArgs y)
                throws Exception
        {
            onResponseMessageReceived(x, y);
        }
    };
    
    private IMethod2<Object, DuplexChannelEventArgs> myOnConnectionOpened = new IMethod2<Object, DuplexChannelEventArgs>()
    {
        @Override
        public void invoke(Object x, DuplexChannelEventArgs y) throws Exception
        {
            onConnectionOpened(x, y);
        }
    };
    
    private IMethod2<Object, DuplexChannelEventArgs> myOnConnectionClosed = new IMethod2<Object, DuplexChannelEventArgs>()
    {
        @Override
        public void invoke(Object x, DuplexChannelEventArgs y) throws Exception
        {
            onConnectionClosed(x, y);
        }
    }; 
    
    
    private String TracedObject()
    {
        String aChannelId = (myUnderlyingDuplexOutputChannel != null) ? myUnderlyingDuplexOutputChannel.getChannelId() : "";
        return "The monitor duplex output channel '" + aChannelId + "' ";
    }
}