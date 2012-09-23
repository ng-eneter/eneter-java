/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable.internal;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.infrastructure.attachable.IAttachableInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;

/**
 * Internal non-api class implementing basic logic for attaching the channel.  
 *
 */
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

    public void detachInputChannel() throws Exception
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

    public boolean isInputChannelAttached()
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
    
    
    private EventHandler<ChannelMessageEventArgs> myMessageReceivedHandler = new EventHandler<ChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ChannelMessageEventArgs e)
        {
            onMessageReceived(sender, e);
        }
    };
    
    private IInputChannel myAttachedInputChannel;
    private Object myLock = new Object();
    
}
