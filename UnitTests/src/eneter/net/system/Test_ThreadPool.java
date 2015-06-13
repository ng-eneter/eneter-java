package eneter.net.system;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

public class Test_ThreadPool
{
    private class Worker implements Runnable
    {
        @Override
        public void run()
        {
            mySendCompletedEvent.set();
        }
    }
    
    @Test
    public void Test_UsingThreadPoolForTimeout() throws Exception
    {
        for (int i = 0; i < 10; ++i)
        {
            synchronized(myWorker)
            {
                mySendCompletedEvent.reset();
                
                // Start writing in another thread.
                ThreadPool.queueUserWorkItem(myWorker);
                
                // Wait until the writing is completed.
                if (!mySendCompletedEvent.waitOne(100000))
                {
                    throw new TimeoutException();
                }
            }
        }
    }
    
    private Worker myWorker = new Worker();
    private ManualResetEvent mySendCompletedEvent = new ManualResetEvent(false);
}
