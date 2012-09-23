/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

/**
 * Equivalent to .NET AutoResetEvent.
 * Threads calling waitOne() wait until 'set' is signaled.
 * Then, only one thread can continue.
 *
 */
public final class AutoResetEvent
{
    public AutoResetEvent(boolean initialState)
    {
        myIsSetFlag = initialState;
    }
    
    public void waitOne() throws InterruptedException
    {
        waitOne(0);
    }
    
    public boolean waitOne(long timeout) throws InterruptedException
    {
        synchronized (myMonitor)
        {
            if (!myIsSetFlag)
            {
                // Release the lock and wait for the 'set' signal.
                myMonitor.wait(timeout);
            }
            
            if (myIsSetFlag)
            {
                // Close "doors" for other threads.
                myIsSetFlag = false;
                
                return true;
            }
            
            // Return false if the waiting was interrupted because of timeout.
            return false;
        }
    }

    public void set()
    {
        synchronized (myMonitor)
        {
            // The event is set.
            myIsSetFlag = true;
            
            // Signal to only one of waiting threads, that the event is set.
            myMonitor.notify();
        }
    }

    public void reset()
    {
        // The event is not set.
        // It means, that if some thread calls waitOne(), it will wait until
        // some other thread calls set().
        myIsSetFlag = false;
    }

    private final Object myMonitor = new Object();
    private volatile boolean myIsSetFlag = false;
}
