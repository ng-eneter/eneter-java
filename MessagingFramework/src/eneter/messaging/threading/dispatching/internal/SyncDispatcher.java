package eneter.messaging.threading.dispatching.internal;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.threading.dispatching.IThreadDispatcher;

public class SyncDispatcher implements IThreadDispatcher
{

    @Override
    public void invoke(Runnable workItem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myQueue)
            {
                if (myQueue.isEmpty())
                {
                    
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

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

                    try
                    {
                        // Execute the job.
                        aJob.run();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                    }

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
    
    
    private Queue<Runnable> myQueue = new ArrayDeque<Runnable>();
    
    private String TracedObject() { return getClass().getSimpleName() + ' '; }
}
