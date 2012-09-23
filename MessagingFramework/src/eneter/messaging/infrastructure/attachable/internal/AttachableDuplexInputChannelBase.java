/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable.internal;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;

/**
 * Internal non-api class implementing basic logic for attaching the channel. 
 *
 */
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

    public boolean isDuplexInputChannelAttached()
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

                if (StringExt.isNullOrEmpty(duplexInputChannel.getChannelId()))
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
    
    private EventHandler<DuplexChannelMessageEventArgs> myMessageReceivedHandler = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageReceived(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverConnectedHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverConnected(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverDisconnectedHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };

    protected abstract String TracedObject();
}
