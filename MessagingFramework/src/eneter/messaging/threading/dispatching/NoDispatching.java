package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;

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
