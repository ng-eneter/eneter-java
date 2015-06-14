/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadFactory;


public class ScalableThreadPool
{
    // Synchronized queue for tasks.
    private static class TaskQueue
    {
        public boolean enqueue(Runnable task)
        {
            // Wait until lock is acquired.
            synchronized (myTasks)
            {
                // Put task to the queue.
                myTasks.add(task);
                
                // Check if there is a thread(s) waiting until a new task is entered.
                boolean anIdleThreadExist = myNumberOfIdleThreads > 0;

                // Release the lock and signal the new task was entered.
                myTasks.notify();
                
                return anIdleThreadExist;
            }
        }
        
        public Runnable dequeue(long timeout) throws InterruptedException
        {
            // Wait until lock is acquired.
            synchronized (myTasks)
            {
                // If the queue with task is not empty then remove the first one and process it.
                if (myTasks.size() > 0)
                {
                    return myTasks.poll();
                }
                
                // The queue with tasks is empty so return the lock and wait until
                // a task is entered and the lock is acquired again or until timeout.
                ++myNumberOfIdleThreads;
                myTasks.wait(timeout);
                
                // We do not know if the waiting was interrupted by re-acquired lock or timeout.
                // Therefore enforce lock acquire.
                synchronized (myTasks)
                {
                    -- myNumberOfIdleThreads;
                    
                    if (myTasks.size() > 0)
                    {
                        return myTasks.poll();
                    }
                    
                    return null;
                }
            }
        }
        
        
        private int myNumberOfIdleThreads;
        private ArrayDeque<Runnable> myTasks = new ArrayDeque<Runnable>();
    }
    
    // Thread living in the pool.
    private class PoolThread
    {
        // Handler processing tasks from the queue.
        private class TaskHandler implements Runnable
        {
            @Override
            public void run()
            {
                while(true)
                {
                    Runnable aTask = null;
                    try
                    {
                        aTask = myTaskQueue.dequeue(myMaxIdleTime);
                    }
                    catch (Exception err)
                    {
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            --myNumberOfThreads;
                        }
                        
                        break;
                    }
                    
                    if (aTask != null)
                    {
                        try
                        {
                            aTask.run();
                        }
                        catch (Exception err)
                        {
                        }
                    }
                    else
                    {
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            if (myNumberOfThreads > myMinNumberOfThreads)
                            {
                                --myNumberOfThreads;
                                
                                break;
                            }
                        }
                    }
                }
                
                //System.out.println("Thread ended: " + Thread.currentThread().getId());
            }
        }
        
        public PoolThread()
        {
            myThread = mythreadFactory.newThread(new TaskHandler());
            
            //System.out.println("Thread started: " + myThread.getId());
        }
        
        public void start()
        {
            myThread.start();
        }
        
       
        private Thread myThread;
    }

    
    public ScalableThreadPool(int minThreads, int maxThreads, int maxIdleTime, ThreadFactory threadFactory)
    {
        myMinNumberOfThreads = minThreads;
        myMaxNumberOfThreads = maxThreads;
        myMaxIdleTime = maxIdleTime;
        mythreadFactory = threadFactory;
    }
    
    public void execute(Runnable task)
    {
        boolean anIdleThreadExist = myTaskQueue.enqueue(task);
        
        if (!anIdleThreadExist)
        {
            synchronized (myNumberOfThreadsManipulator)
            {
                if (myNumberOfThreads < myMaxNumberOfThreads)
                {
                    ++myNumberOfThreads;
                   
                    PoolThread aThread = new PoolThread();
                    aThread.start();
                }
            }
        }
    }
    
    
    private ThreadFactory mythreadFactory;
    private int myMinNumberOfThreads;
    private int myMaxNumberOfThreads;
    private int myMaxIdleTime;
    
    private Object myNumberOfThreadsManipulator = new Object();
    private int myNumberOfThreads;
    
    private TaskQueue myTaskQueue = new TaskQueue();
}
