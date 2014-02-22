/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.internal.SyncDispatcher;

/**
 * Dispatcher that queues callback methods and invokes them one by one from one thread.
 *
 */
public class SyncDispatching implements IThreadDispatcherProvider
{
    /**
     * Constructs the dispatcher provider which getDispatcher() always creates the new dispatcher.
     */
    public SyncDispatching()
    {
        this(false);
    }
    
    /**
     * Constructs the dispatcher provider.
     * @param isDispatcherShared if true then it always returns the same instance of the dispatcher. It means the dispatcher.
     *    If false the then getDispatcher() always creates the new instance. 
     */
    public SyncDispatching(boolean isDispatcherShared)
    {
        if (isDispatcherShared)
        {
            mySharedDispatcher = new SyncDispatcher();
        }
    }

    /**
     * Returns dispatcher that queues callback methods and processes them one by one from one threads.
     */
    @Override
    public IThreadDispatcher getDispatcher()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return (mySharedDispatcher != null) ? mySharedDispatcher : new SyncDispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IThreadDispatcher mySharedDispatcher;
}
