/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2016 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

public class EneterTimer
{
    public EneterTimer(Runnable callback)
    {
        this (callback, "Eneter.Timer");
    }
    
    public EneterTimer(Runnable callback, String name)
    {
        myTickCallback = callback;
        
        myWaitingThreadPool = new ScalableThreadPool(0, 1, 1000, name + ".Wait");
        myTickingThreadPool = new ScalableThreadPool(0, 100, 10000, name + ".Tick");
    }
    
    public void change(long millisecondsTimeout) throws InterruptedException
    {
        //using (EneterTrace.Entering())
        //{
            synchronized (myScheduleLock)
            {
                // Release the previous waiting.
                myTimeElapsedEvent.set();

                if (millisecondsTimeout > -1)
                {
                    // Set the new waiting.
                    myMillisecondsTimeout = millisecondsTimeout;
                    myWaitingThreadPool.execute(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                doWaiting();
                            }
                            catch (Exception err)
                            {
                                // n.a.
                            }
                        }
                    });

                    // Wait until the waiting started.
                    // This is to avoid multiple waitings cumulated the queue.
                    myWaitingStartedEvent.waitOne(5000);
                }
            }
        //}
    }
    
    private void doWaiting() throws Exception
    {
        // Indicate the waiting started.
        myWaitingStartedEvent.set();

        // Wait until the time elapses or until the waiting is canceled.
        myTimeElapsedEvent.reset();
        
        if (myTimeElapsedEvent.waitOne(myMillisecondsTimeout))
        {
            // if the waiting is canceled then just return.
            return;
        }

        // The time ellapsed so call the tick callback.
        // Execute it in a different thread so that if the tick-callback calls EneterTimer.Change
        // the deadlock will not happen.
        myTickingThreadPool.execute(myTickCallback);
    }

    private Runnable myTickCallback;
    private long myMillisecondsTimeout;
    private ManualResetEvent myTimeElapsedEvent = new ManualResetEvent(false);
    private AutoResetEvent myWaitingStartedEvent = new AutoResetEvent(false);
    private Object myScheduleLock = new Object();
    private ScalableThreadPool myWaitingThreadPool;
    private ScalableThreadPool myTickingThreadPool;
}
