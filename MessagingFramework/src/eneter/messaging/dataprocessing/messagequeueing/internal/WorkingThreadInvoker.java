/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.messagequeueing.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.net.system.internal.IMethod;



/**
 * Represents the thread processing enqueued Activity delegates in the single working thread.
 *
 */
public class WorkingThreadInvoker implements IInvoker
{
    public WorkingThreadInvoker()
    {
        this ("");
    }

    public WorkingThreadInvoker(String workerName)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myWorkingThreadName = workerName;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void start()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (myThreadPool != null)
                {
                    String aMessage = TracedObject() + " is already running.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                try
                {
                    // Start the single thread with the queue.
                    myThreadPool = Executors.newSingleThreadExecutor();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + " failed to start the working thread.", err);
                    stop();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stop()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (myThreadPool != null)
                {
                    try
                    {
                        myThreadPool.shutdownNow();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + " failed to stop the thread processing messages in the queue.", err);
                    }
                    
                    myThreadPool = null;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void invoke(final IMethod workItem) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                myThreadPool.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            workItem.invoke();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private String myWorkingThreadName = "";
    private ExecutorService myThreadPool;
    private Object myLock = new Object();
    

    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myWorkingThreadName +"' ";
    }
}
