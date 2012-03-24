/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Provides .NET style thread pool for Eneter Messaging needs.
 * The class provides one single thread pool for the Eneter Messaging Framework.
 */
public final class ThreadPool
{
    /**
     * Factory creating daemon threads for the thread pool.
     * The daemon thread do not block the the application when shutdown. 
     */
    private static class DaemonThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread aNewThread = new Thread(r);
            aNewThread.setDaemon(true);
            return aNewThread;
        }
    }
    
    public static void queueUserWorkItem(Runnable callback)
    {
        myThreadPool.execute(callback);
    }
    
    private static ExecutorService myThreadPool = Executors.newFixedThreadPool(400, new DaemonThreadFactory());
}
