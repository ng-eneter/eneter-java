package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import android.os.Handler;

/**
 * Invokes one by one using Android Handler mechanism (e.g. to invoke in the UI thread).
 * 
 * This dispatcher is available only in Eneter for Android and is intended to route messages
 * and events to the thread associated with the provided Handler.
 *
 */
public class AndroidDispatching implements IThreadDispatcherProvider
{
    private static class AndroidDispatcher implements IThreadDispatcher
    {
        public AndroidDispatcher(Handler threadDispatcher)
        {
            myDispatcher = threadDispatcher;
        }
        
        @Override
        public void invoke(Runnable workItem)
        {
            myDispatcher.post(workItem);
        }
        
        private Handler myDispatcher;
    }
    
    /**
     * Constructs dispatcher.
     * @param threadDispatcher handler used for dispatching.
     */
    public AndroidDispatching(Handler threadDispatcher)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myThreadDispatcher = new AndroidDispatcher(threadDispatcher);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myThreadDispatcher;
    }

    private IThreadDispatcher myThreadDispatcher;
}
