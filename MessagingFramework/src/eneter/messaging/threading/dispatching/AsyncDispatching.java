/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.threading.internal.ThreadPool;

/**
 * Invokes asynchronously by routing to a thread from the thread-pool (each method can be executed in a separate thread).
 *
 */
public class AsyncDispatching implements IThreadDispatcherProvider
{
    private static class AsyncDispatcher implements IThreadDispatcher
    {
        @Override
        public void invoke(Runnable workItem)
        {
            ThreadPool.queueUserWorkItem(workItem);
        }
    }

    /**
     * Returns dispatcher which invokes asynchronously in a thread from the thread-pool.
     */
    @Override
    public IThreadDispatcher getDispatcher()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDispatcher;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private static AsyncDispatcher myDispatcher = new AsyncDispatcher();
}
