/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching.internal;

import java.util.concurrent.ThreadFactory;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.threading.internal.ScalableThreadPool;

public class SyncDispatcher implements IThreadDispatcher
{
    @Override
    public void invoke(Runnable workItem)
    {
        if (myDispatchingInfo != null)
        {
            EneterTrace.debug(myDispatchingInfo);
        }
        myWorkingThread.execute(workItem);
    }

    
    // Ensures sequential processing of work-items by one thread.
    private ScalableThreadPool myWorkingThread = new ScalableThreadPool(0, 1, 5000, new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread aNewThread = new Thread(r, "Eneter.SyncDispatcher");
            
            // Thread shall not block the application to shutdown.
            aNewThread.setDaemon(true);
            
            // Store thread id for diagnostic purposes.
            myDispatchingInfo = "To ~" + Long.toString(aNewThread.getId());
            
            EneterTrace.debug(myDispatchingInfo);
            
            return aNewThread;
        }
    });
    
    private String myDispatchingInfo;
}
