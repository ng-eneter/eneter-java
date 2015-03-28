/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

/**
 * Equivalent to .NET ManualResetEvent.
 * Threads calling waitOne() wait until 'set' is signaled.
 * Then, all waiting threads can continue.
 *
 */
public final class ManualResetEvent
{
    public ManualResetEvent(boolean initialState)
    {
        myIsSetFlag = initialState;
    }
    
    public void waitOne() throws InterruptedException
    {
        waitOne(0);
    }
    
    /**
     * 
     * @param timeout 0 infinite waiting.
     * @return
     * @throws InterruptedException
     */
    public boolean waitOne(long timeout) throws InterruptedException
    {
        synchronized (myMonitor)
        {
            if (!myIsSetFlag)
            {
                // Release the lock and wait for the 'set' signal.
                myMonitor.wait(timeout);
                
                // Return false if the waiting was interrupted because of timeout.
                return myIsSetFlag; 
            }
            
            return true;
        }
    }

    public void set()
    {
        synchronized (myMonitor)
        {
            // The event is set.
            myIsSetFlag = true;
            
            // Signal to all waiting threads, the event is set.
            myMonitor.notifyAll();
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
