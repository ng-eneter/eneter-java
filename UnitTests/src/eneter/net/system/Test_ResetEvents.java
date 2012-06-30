package eneter.net.system;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.net.system.threading.*;

public class Test_ResetEvents
{
    @Test
    public void AutoResetEvent()
        throws Exception
    {
        // Create AutoResetEvent so that the thread will wait.
        final AutoResetEvent anAutoResetEvent = new AutoResetEvent(false);
        
        final int[] aCompletedAmount = new int[1]; 
        
        // Create 10 methods running in different threads.
        Runnable[] aRunnables = new Runnable[10];
        for (int i = 0; i < aRunnables.length; ++i)
        {
            aRunnables[i] = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        // The method just waits for the signal.
                        anAutoResetEvent.waitOne();
                        
                        // Increase amount of completed threads.
                        ++aCompletedAmount[0];
                    } catch (InterruptedException e)
                    {
                    }
                }
            };
        }
        
        // Execute all threads.
        for (Runnable r : aRunnables)
        {
            ThreadPool.queueUserWorkItem(r);
        }
        
        // Allow some time so that threads can stop at 'wait()'.
        Thread.sleep(50);

        assertEquals(0, aCompletedAmount[0]);
        
        // Signal 10 times. Threads should go one by one.
        for (int i = 0; i < aRunnables.length; ++i)
        {
            anAutoResetEvent.set();
        
            // Allow some time so that threads can proceed.
            Thread.sleep(50);
        
            // Only one thread finished.
            assertEquals(i + 1, aCompletedAmount[0]);
        }
    }
    
    @Test
    public void AutoResetEvent_Timeout()
        throws Exception
    {
        AutoResetEvent anAutoResetEvent = new AutoResetEvent(false);

        // Signal does will not be set, so the timeout must take place
        // and the false should be returned.
        boolean aResult = anAutoResetEvent.waitOne(50);
        
        assertFalse(aResult);
    }
    
    @Test
    public void ManualResetEvent()
        throws Exception
    {
        // Create AutoResetEvent so that the thread will wait.
        final ManualResetEvent aManualResetEvent = new ManualResetEvent(false);
        
        final int[] aCompletedAmount = new int[1]; 
        
        // Create 10 methods running in different threads.
        Runnable[] aRunnables = new Runnable[10];
        for (int i = 0; i < aRunnables.length; ++i)
        {
            aRunnables[i] = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        // The method just waits for the signal.
                        aManualResetEvent.waitOne();
                        
                        // Increase amount of completed threads.
                        synchronized (aCompletedAmount)
                        {
                            ++aCompletedAmount[0];
                        }
                    } catch (InterruptedException e)
                    {
                    }
                }
            };
        }
        
        // Execute all threads.
        for (Runnable r : aRunnables)
        {
            ThreadPool.queueUserWorkItem(r);
        }
        
        // Allow some time so that threads can stop at 'wait()'.
        Thread.sleep(50);

        assertEquals(0, aCompletedAmount[0]);
        
        aManualResetEvent.set();
        
        // Allow some time so that threads can proceed.
        Thread.sleep(50);
        
        // All threads should be finished.
        assertEquals(aRunnables.length, aCompletedAmount[0]);
    }
    
    @Test
    public void ManualResetEvent_Timeout()
        throws Exception
    {
        ManualResetEvent aManualResetEvent = new ManualResetEvent(false);

        // Signal does will not be set, so the timeout must take place
        // and the false should be returned.
        boolean aResult = aManualResetEvent.waitOne(50);
        
        assertFalse(aResult);
    }
}
