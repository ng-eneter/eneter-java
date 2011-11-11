/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;

public abstract class AttachableDuplexOutputChannelBase implements IAttachableDuplexOutputChannel
{
    public void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelManipulatorLock)
            {
                attach(duplexOutputChannel);
    
                try
                {
                    myAttachedDuplexOutputChannel.openConnection();
                }
                catch (Exception err)
                {
                    try
                    {
                        detachDuplexOutputChannel();
                    }
                    catch (Exception err2)
                    {
                        // Ignore exception in exception.
                    }
    
                    String aMessage = TracedObject() + ErrorHandler.OpenConnectionFailure;
                    EneterTrace.error(aMessage, err);
                    throw new IllegalStateException(aMessage);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelManipulatorLock)
            {
                if (myAttachedDuplexOutputChannel != null)
                {
                    try
                    {
                        // Try to notify, the connection is closed.
                        myAttachedDuplexOutputChannel.closeConnection();
                    }
                    finally
                    {
                        // Detach the event handler.
                        myAttachedDuplexOutputChannel.responseMessageReceived().unsubscribe(myResponseMessageHandler);
                        myAttachedDuplexOutputChannel = null;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public boolean isDuplexOutputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelManipulatorLock)
            {
                return myAttachedDuplexOutputChannel != null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void attach(IDuplexOutputChannel duplexOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelManipulatorLock)
            {
                if (duplexOutputChannel == null)
                {
                    String aMessage = TracedObject() + "failed to attach the duplex output channel, because the input parameter 'duplexOutputChannel' is null.";
                    EneterTrace.error(aMessage);
                    throw new InvalidParameterException(aMessage);
                }

                if (duplexOutputChannel.getChannelId() == null || duplexOutputChannel.getChannelId().isEmpty())
                {
                    String aMessage = TracedObject() + "failed to attach the duplex output channel, because the input parameter 'duplexOutputChannel' has empty or null channel id.";
                    EneterTrace.error(aMessage);
                    throw new InvalidParameterException(aMessage);
                }

                if (isDuplexOutputChannelAttached())
                {
                    String aMessage = TracedObject() + "failed to attach the duplex output channel '" + duplexOutputChannel.getChannelId() + "' because the duplex output channel is already attached.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myAttachedDuplexOutputChannel = duplexOutputChannel;
                myAttachedDuplexOutputChannel.responseMessageReceived().subscribe(myResponseMessageHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    protected abstract void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e);


    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        return myAttachedDuplexOutputChannel;
    }

    protected Object myDuplexOutputChannelManipulatorLock = new Object();
    
    private IDuplexOutputChannel myAttachedDuplexOutputChannel;
    
    private IMethod2<Object, DuplexChannelMessageEventArgs> myResponseMessageHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, DuplexChannelMessageEventArgs e)
                throws Exception
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    protected abstract String TracedObject();
}
