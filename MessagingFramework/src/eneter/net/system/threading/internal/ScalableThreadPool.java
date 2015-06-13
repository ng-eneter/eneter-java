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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;


public class ScalableThreadPool
{
    // Synchronized queue for tasks.
    private static class TaskQueue
    {
        public void enqueue(Runnable task)
        {
            myLock.lock();
            try
            {
                myTasks.add(task);
                myTaskEnqued.signal();
            }
            finally
            {
                myLock.unlock();
            }
        }
        
        public Runnable dequeue(long timeout) throws InterruptedException
        {
            myLock.lock();
            try
            {
                if (!myTasks.isEmpty())
                {
                    return myTasks.poll();
                }
                
                if (myTaskEnqued.await(timeout, TimeUnit.MILLISECONDS))
                {
                    return myTasks.poll();
                }
                
                return null;
            }
            finally
            {
                myLock.unlock();
            }
        }
        
        
        private ReentrantLock myLock = new ReentrantLock();
        private Condition myTaskEnqued = myLock.newCondition();
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
                synchronized (myNumberOfThreadsManipulator)
                {
                    ++myNumberOfThreads;
                    ++myNumberOfIdleThreads;
                }
                
                while(true)
                {
                    Runnable aTask = null;
                    try
                    {
                        aTask = myTaskQueue.dequeue(100);
                    }
                    catch (Exception err)
                    {
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            --myNumberOfThreads;
                            --myNumberOfIdleThreads;
                        }
                        
                        break;
                    }
                    
                    if (aTask != null)
                    {
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            --myNumberOfIdleThreads;
                        }
                        
                        try
                        {
                            aTask.run();
                        }
                        catch (Exception err)
                        {
                        }
                        
                        myIdleTime = 0;
                        
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            ++myNumberOfIdleThreads;
                        }
                    }
                    else
                    {
                        myIdleTime += 100;
                        
                        if (myIdleTime >= myMaxIdleTime)
                        {
                            synchronized (myNumberOfThreadsManipulator)
                            {
                                if (myNumberOfThreads > myMinNumberOfThreads)
                                {
                                    --myNumberOfThreads;
                                    --myNumberOfIdleThreads;
                                    
                                    break;
                                }
                                else
                                {
                                    myIdleTime = 0;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public PoolThread()
        {
            myThread = mythreadFactory.newThread(new TaskHandler());
        }
        
        public void start()
        {
            myThread.start();
        }
        
       
        private Thread myThread;
        private int myIdleTime;
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
        myTaskQueue.enqueue(task);
        
        synchronized (myNumberOfThreadsManipulator)
        {
            if (myNumberOfThreads < myMaxNumberOfThreads &&
                myNumberOfIdleThreads == 0)
            {
                PoolThread aThread = new PoolThread();
                aThread.start();
            }
        }
    }
    
    
    private ThreadFactory mythreadFactory;
    private int myMinNumberOfThreads;
    private int myMaxNumberOfThreads;
    private int myMaxIdleTime;
    
    private Object myNumberOfThreadsManipulator = new Object();
    private int myNumberOfThreads;
    private int myNumberOfIdleThreads;
    
    private TaskQueue myTaskQueue = new TaskQueue();
}
