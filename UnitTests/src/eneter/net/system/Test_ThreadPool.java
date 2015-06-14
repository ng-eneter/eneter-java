package eneter.net.system;

import static org.junit.Assert.*;

import java.util.concurrent.ThreadFactory;

import helper.EventWaitHandleExt;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ScalableThreadPool;


public class Test_ThreadPool
{
    private class MyThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread aThread = new Thread(r, "Eneter.TestPool");
            aThread.setDaemon(true);
            
            //System.out.println("My thread id: " + aThread.getId());
            
            return aThread;
        }
    }
   
    
    @Test
    public void Test_Perform1000000_Tasks() throws Exception
    {
        System.out.println("Test started.");
        
        ScalableThreadPool aThreadPool = new ScalableThreadPool(0, 300, 3, new MyThreadFactory());
        
        final ManualResetEvent aTasksCompletedEvent = new ManualResetEvent(false);
        final int[] aCompletedTasks = { 0 };
        
        for (int i = 0; i < 1000000; ++i)
        {
            aThreadPool.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(2);
                    }
                    catch (InterruptedException e)
                    {
                    }
                    
                    int k;
                    synchronized (aCompletedTasks)
                    {
                        k = ++aCompletedTasks[0];
                    }
                    
                    //EneterTrace.info(String.valueOf(k));
                    
                    if (k == 1000000)
                    {
                        aTasksCompletedEvent.set();
                    }
                }
            });
        }
        
        EventWaitHandleExt.waitIfNotDebugging(aTasksCompletedEvent, 300000);
        
        Thread.sleep(1000);
    }
    
}
