/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.net.system.threading.internal;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;


class ScalableThreadPool
{
    public static class TaskQueue
    {
        public void enqueue(Runnable task)
        {
            myLock.lock();
            try
            {
                myMessageQueue.add(task);
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
                if (!myMessageQueue.isEmpty())
                {
                    return myMessageQueue.poll();
                }
                
                if (myTaskEnqued.await(timeout, TimeUnit.MILLISECONDS))
                {
                    return myMessageQueue.poll();
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
        private ArrayDeque<Runnable> myMessageQueue = new ArrayDeque<Runnable>();
    }
    
    private class WorkingThread
    {
        private class TaskHandler implements Runnable
        {
            @Override
            public void run()
            {
                synchronized (myNumberOfThreadsManipulator)
                {
                    ++myCurrentNumberOfThreads;
                    ++myNumberOfIdleThreds;
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
                            --myCurrentNumberOfThreads;
                            --myNumberOfIdleThreds;
                        }
                        
                        break;
                    }
                    
                    if (aTask != null)
                    {
                        synchronized (myNumberOfThreadsManipulator)
                        {
                            --myNumberOfIdleThreds;
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
                            ++myNumberOfIdleThreds;
                        }
                    }
                    else
                    {
                        myIdleTime += 100;
                        
                        if (myIdleTime >= myMaxIdleTime)
                        {
                            synchronized (myNumberOfThreadsManipulator)
                            {
                                if (myCurrentNumberOfThreads > myMinNumberOfThreads)
                                {
                                    --myCurrentNumberOfThreads;
                                    --myNumberOfIdleThreds;
                                    
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public WorkingThread()
        {
            myThread = new Thread(new TaskHandler(), "Eneter.ThreadPool");
            myThread.setDaemon(true);
        }
        
        public void run()
        {
            myThread.start();
        }
        
       
        private Thread myThread;
        private int myIdleTime;
    }

    
    public ScalableThreadPool(int minThreads, int maxThreads, int maxIdleTime)
    {
        myMinNumberOfThreads = minThreads;
        myMaxNumberOfThreads = maxThreads;
        myMaxIdleTime = maxIdleTime;
    }
    
    public void execute(Runnable task)
    {
        myTaskQueue.enqueue(task);
        
        synchronized (myNumberOfThreadsManipulator)
        {
            if (myCurrentNumberOfThreads < myMaxNumberOfThreads &&
                myNumberOfIdleThreds == 0)
            {
                WorkingThread aThread = new WorkingThread();
                aThread.run();
            }
        }
    }
    
    
    private int myMinNumberOfThreads;
    private int myMaxNumberOfThreads;
    private int myMaxIdleTime;
    
    private Object myNumberOfThreadsManipulator = new Object();
    private int myCurrentNumberOfThreads;
    private int myNumberOfIdleThreds;
    
    private TaskQueue myTaskQueue = new TaskQueue();
}
