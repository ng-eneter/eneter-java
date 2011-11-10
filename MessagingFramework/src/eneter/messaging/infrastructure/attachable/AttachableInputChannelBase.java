package eneter.messaging.infrastructure.attachable;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;

public abstract class AttachableInputChannelBase implements IAttachableInputChannel
{
    public void attachInputChannel(IInputChannel inputChannel)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (isInputChannelAttached())
                {
                    String aMessage = "The input channel is already attached.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myAttachedInputChannel = inputChannel;

                myAttachedInputChannel.messageReceived().subscribe(myMessageReceivedHandler);

                try
                {
                    myAttachedInputChannel.startListening();
                }
                catch (Exception err)
                {
                    // Clean after the failure.
                    myAttachedInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                    myAttachedInputChannel = null;

                    throw err;
                }
                catch (Error err)
                {
                    // Clean after the failure.
                    myAttachedInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                    myAttachedInputChannel = null;

                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (isInputChannelAttached())
                {
                    try
                    {
                        if (myAttachedInputChannel.isListening())
                        {
                            myAttachedInputChannel.stopListening();
                        }
                    }
                    finally
                    {
                        myAttachedInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                        myAttachedInputChannel = null;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public Boolean isInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                return myAttachedInputChannel != null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public IInputChannel getAttachedInputChannel()
    { 
        return myAttachedInputChannel;
    }

    protected abstract void onMessageReceived(Object sender, ChannelMessageEventArgs e);
    
    
    private IMethod2<Object, ChannelMessageEventArgs> myMessageReceivedHandler = new IMethod2<Object, ChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, ChannelMessageEventArgs e) throws Exception
        {
            onMessageReceived(sender, e);
        }
    };
    
    private IInputChannel myAttachedInputChannel;
    private Object myLock = new Object();
    
}
