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
     * Constructs dispatching where each getDispatcher() will return new instance of the dispatcher.
     */
    public SyncDispatching()
    {
        this(false);
    }
    
    /**
     * Constructs the dispatcher provider.
     * @param isDispatcherShared true - getDispatcher() will return always the same instance of the dispatcher. It means all dispatchers returned from
     * getDispatcher() will sync incoming methods using the same queue. <br/>
     * false - getDispatcher() will return always the new instance of the dispatcher. It means each dispatcher returned from
     * getDispatcher() will use its own synchronization queue.
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
     * 
     * If SyncDispatching was created with isDispatcherShared true then it always returns the same instance
     * of the thread dispatcher. Otherwise it always creates the new one. 
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
