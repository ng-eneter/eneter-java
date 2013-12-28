package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.internal.SyncDispatcher;

public class SyncDispatching implements IThreadDispatcherProvider
{
    public SyncDispatching()
    {
        this(false);
    }
    
    public SyncDispatching(boolean isDispatcherShared)
    {
        if (isDispatcherShared)
        {
            mySharedDispatcher = new SyncDispatcher();
        }
    }

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
