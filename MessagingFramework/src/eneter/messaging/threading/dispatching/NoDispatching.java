/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Invokes directly without routing.
 *
 */
public class NoDispatching implements IThreadDispatcherProvider
{
    private static class DefaultDispatcher implements IThreadDispatcher
    {
        @Override
        public void invoke(Runnable workItem)
        {
            workItem.run();
        }
    }

    /**
     * Returns dispatcher which invokes directly without routing into a thread.
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

    private static DefaultDispatcher myDispatcher = new DefaultDispatcher();
}
