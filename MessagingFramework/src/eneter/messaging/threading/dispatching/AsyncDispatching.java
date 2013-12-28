package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.threading.internal.ThreadPool;


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
