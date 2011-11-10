package eneter.messaging.infrastructure.attachable;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;

public abstract class AttachableDuplexInputChannelBase implements IAttachableDuplexInputChannel
{
    public void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelManipulatorLock)
            {
                Attach(duplexInputChannel);

                try
                {
                    myAttachedDuplexInputChannel.startListening();
                }
                catch (Exception err)
                {
                    try
                    {
                        detachDuplexInputChannel();
                    }
                    catch (Exception err2)
                    {
                        // Ignore exception in exception.
                    }

                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelManipulatorLock)
            {
                if (myAttachedDuplexInputChannel != null)
                {
                    try
                    {
                        myAttachedDuplexInputChannel.stopListening();
                    }
                    finally
                    {
                        myAttachedDuplexInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                        myAttachedDuplexInputChannel.responseReceiverConnected().unsubscribe(myResponseReceiverConnectedHandler);
                        myAttachedDuplexInputChannel.responseReceiverDisconnected().unsubscribe(myResponseReceiverDisconnectedHandler);
                        myAttachedDuplexInputChannel = null;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public Boolean isDuplexInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelManipulatorLock)
            {
                return myAttachedDuplexInputChannel != null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    private void Attach(IDuplexInputChannel duplexInputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelManipulatorLock)
            {
                if (duplexInputChannel == null)
                {
                    String aMessage = TracedObject() + "failed to attach duplex input channel because the input parameter 'duplexInputChannel' is null.";
                    EneterTrace.error(aMessage);
                    throw new InvalidParameterException(aMessage);
                }

                if (duplexInputChannel.getChannelId() == null || duplexInputChannel.getChannelId().isEmpty())
                {
                    String aMessage = TracedObject() + "failed to attach duplex input channel because the input parameter 'duplexInputChannel' has empty or null channel id.";
                    EneterTrace.error(aMessage);
                    throw new InvalidParameterException(aMessage);
                }

                if (isDuplexInputChannelAttached())
                {
                    String aMessage = TracedObject() + "failed to attach duplex input channel '" + duplexInputChannel.getChannelId() + "' because the channel is already attached.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myAttachedDuplexInputChannel = duplexInputChannel;

                myAttachedDuplexInputChannel.messageReceived().subscribe(myMessageReceivedHandler);
                myAttachedDuplexInputChannel.responseReceiverConnected().subscribe(myResponseReceiverConnectedHandler);
                myAttachedDuplexInputChannel.responseReceiverDisconnected().subscribe(myResponseReceiverDisconnectedHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    protected abstract void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e);

    protected abstract void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e);

    protected abstract void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e);


    public IDuplexInputChannel getAttachedDuplexInputChannel()
    {
        return myAttachedDuplexInputChannel;
    }

    protected Object myDuplexInputChannelManipulatorLock = new Object();
    
    private IDuplexInputChannel myAttachedDuplexInputChannel;
    
    private IMethod2<Object, DuplexChannelMessageEventArgs> myMessageReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, DuplexChannelMessageEventArgs e)
                throws Exception
        {
            onMessageReceived(sender, e);
        }
    };
    
    private IMethod2<Object, ResponseReceiverEventArgs> myResponseReceiverConnectedHandler = new IMethod2<Object, ResponseReceiverEventArgs>()
    {
        @Override
        public void invoke(Object sender, ResponseReceiverEventArgs e)
                throws Exception
        {
            onResponseReceiverConnected(sender, e);
        }
    };
    
    private IMethod2<Object, ResponseReceiverEventArgs> myResponseReceiverDisconnectedHandler = new IMethod2<Object, ResponseReceiverEventArgs>()
    {
        
        @Override
        public void invoke(Object sender, ResponseReceiverEventArgs e)
                throws Exception
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };

    protected abstract String TracedObject();
}
