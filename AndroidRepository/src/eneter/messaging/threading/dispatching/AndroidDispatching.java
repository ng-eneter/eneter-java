package eneter.messaging.threading.dispatching;

import eneter.messaging.diagnostic.EneterTrace;
import android.os.Handler;

/**
 * Invokes using Android Handler mechanism (e.g. to invoke in Android application UI thread). 
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
