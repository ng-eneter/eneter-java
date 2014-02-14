package eneter.messaging.threading.dispatching.internal;

import java.util.ArrayDeque;
import java.util.Queue;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.threading.internal.ThreadPool;

public class SyncDispatcher implements IThreadDispatcher
{
    private class Worker implements Runnable
    {
        @Override
        public void run()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                try
                {
                    Runnable aJob;

                    while (true)
                    {
                        // Take the job from the queue.
                        synchronized (myQueue)
                        {
                            if (myQueue.isEmpty())
                            {
                                return;
                            }

                            aJob = myQueue.remove();
                        }

                        // Execute the job.
                        // Note: run() does not throw an exception.
                        aJob.run();

                        // If it was the last job then the working thread can end.
                        synchronized (myQueue)
                        {
                            if (myQueue.isEmpty())
                            {
                                return;
                            }
                        }
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed in processing the working queue.", err);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
    }

    @Override
    public void invoke(Runnable workItem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myQueue)
            {
                // If the queue is empty, then start also the thread that will process messages.
                // If the queue is not empty, the  processing thread already exists.
                if (myQueue.isEmpty())
                {
                    ThreadPool.queueUserWorkItem(myWorker);
                }
                
                // Enqueue the action to be executed.
                myQueue.add(workItem);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private Worker myWorker = new Worker();
    private Queue<Runnable> myQueue = new ArrayDeque<Runnable>();
    
    private String TracedObject() { return getClass().getSimpleName() + ' '; }
}
