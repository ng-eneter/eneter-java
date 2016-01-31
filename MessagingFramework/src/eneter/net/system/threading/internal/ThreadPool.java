/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

import java.util.concurrent.ThreadFactory;

/**
 * Provides .NET style thread pool for Eneter Messaging needs.
 * The class provides one single thread pool for the Eneter Messaging Framework.
 */
public final class ThreadPool
{
    public static void queueUserWorkItem(Runnable callback)
    {
        myThreadPool.execute(callback);
    }
    
    private static ScalableThreadPool myThreadPool = new ScalableThreadPool(10, 400, 5000);
}
