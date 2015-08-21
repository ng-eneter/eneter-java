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
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;

/**
 * Internal non-api class implementing basic logic for attaching the channel. 
 *
 */
public abstract class AttachableDuplexOutputChannelBase implements IAttachableDuplexOutputChannel
{
    public void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexOutputChannelManipulatorLock.lock();
            try
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
    
                    String aMessage = TracedObject() + ErrorHandler.FailedToOpenConnection;
                    EneterTrace.error(aMessage, err);
                    throw err;
                }
            }
            finally
            {
                myDuplexOutputChannelManipulatorLock.unlock();
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
            myDuplexOutputChannelManipulatorLock.lock();
            try
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
                        myAttachedDuplexOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpenedEvent);
                        myAttachedDuplexOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosedEvent);
                        myAttachedDuplexOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceivedEvent);
                        myAttachedDuplexOutputChannel = null;
                    }
                }
            }
            finally
            {
                myDuplexOutputChannelManipulatorLock.unlock();
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
            myDuplexOutputChannelManipulatorLock.lock();
            try
            {
                return myAttachedDuplexOutputChannel != null;
            }
            finally
            {
                myDuplexOutputChannelManipulatorLock.unlock();
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
            myDuplexOutputChannelManipulatorLock.lock();
            try
            {
                if (duplexOutputChannel == null)
                {
                    String aMessage = TracedObject() + "failed to attach the duplex output channel, because the input parameter 'duplexOutputChannel' is null.";
                    EneterTrace.error(aMessage);
                    throw new InvalidParameterException(aMessage);
                }

                if (StringExt.isNullOrEmpty(duplexOutputChannel.getChannelId()))
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
                myAttachedDuplexOutputChannel.connectionOpened().subscribe(myOnConnectionOpenedEvent);
                myAttachedDuplexOutputChannel.connectionClosed().subscribe(myOnConnectionClosedEvent);
                myAttachedDuplexOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceivedEvent);
            }
            finally
            {
                myDuplexOutputChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    protected abstract void onConnectionOpened(Object sender, DuplexChannelEventArgs e);
    protected abstract void onConnectionClosed(Object sender, DuplexChannelEventArgs e);
    protected abstract void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e);


    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        return myAttachedDuplexOutputChannel;
    }

    protected ThreadLock myDuplexOutputChannelManipulatorLock = new ThreadLock();
    
    private IDuplexOutputChannel myAttachedDuplexOutputChannel;
    
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpenedEvent = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionOpened(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosedEvent = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceivedEvent = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    protected abstract String TracedObject();
}
