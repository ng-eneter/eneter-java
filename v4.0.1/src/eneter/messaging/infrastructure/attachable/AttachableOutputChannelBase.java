/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Internal non-api class implementing basic logic for attaching the channel.  
 *
 */
public abstract class AttachableOutputChannelBase implements IAttachableOutputChannel
{
    public void attachOutputChannel(IOutputChannel outputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized(myLock)
            {
                if (isOutputChannelAttached())
                {
                    String aMessage = "The output channel is already attached. The currently attached channel id is '" + myAttachedOutputChannel.getChannelId() + "'.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myAttachedOutputChannel = outputChannel;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized(myLock)
            {
                myAttachedOutputChannel = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public boolean isOutputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized(myLock)
            {
                return myAttachedOutputChannel != null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public IOutputChannel getAttachedOutputChannel()
    {
        return myAttachedOutputChannel;
    }
    
    
    private IOutputChannel myAttachedOutputChannel;
    private Object myLock = new Object();
}
