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
 * Dispatcher that invokes the callback methods as is - without marshaling them into a particular thread. 
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
     * Returns dispatcher that invokes the callback method immediately without marshaling into a thread.
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
