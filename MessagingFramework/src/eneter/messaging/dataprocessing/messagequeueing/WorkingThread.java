/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */
 
package eneter.messaging.dataprocessing.messagequeueing;

import eneter.messaging.dataprocessing.messagequeueing.internal.WorkingThreadInvoker;
import eneter.messaging.diagnostic.*;
import eneter.net.system.IMethod;
import eneter.net.system.IMethod1;

/**
 * Implements the thread that has the message queue.
 * If a message is put to the queue, the thread removes it from the queue and calls a user defined
 * method to handle it.
 *
 * @param <_MessageType> type of the message processed by the thread
 */
public class WorkingThread<_MessageType>
{
    /**
     * Constructs the working thread.
     */
    public WorkingThread()
    {
        this("");
    }
    
    /**
     * Constructs the working thread with the specified name.
     * @param workingThreadName name of the working thread
     */
    public WorkingThread(String workingThreadName)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myWorkingThreadName = workingThreadName;
            myWorker = new WorkingThreadInvoker(workingThreadName);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Registers the method handling messages from the queue and starts the thread reading messages from the queue.
     * @param messageHandler Callback called from the working thread to process the message
     */
    public void registerMessageHandler(IMethod1<_MessageType> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (myMessageHandler != null)
                {
                    String aMessage = TracedObject() + "has already registered the message handler.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                myMessageHandler = messageHandler;
                
                // Start the single thread with the queue.
                myWorker.start();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /**
     * Unregisters the method handling messages from the queue and stops the thread reading messages.
     */
    public void unregisterMessageHandler()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                myWorker.stop();
                myMessageHandler = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Puts the message to the queue.
     * @param message message
     * @throws Exception 
     */
    public void enqueueMessage(final _MessageType message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                if (myMessageHandler == null)
                {
                    String aMessage = TracedObject() + "failed to enqueue the message because the message handler is not registered.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
    
                myWorker.invoke(new IMethod()
                {
                    @Override
                    public void invoke() throws Exception
                    {
                        myMessageHandler.invoke(message);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /// <summary>
    /// Handler called to process the message from the queue.
    /// </summary>
    private IMethod1<_MessageType> myMessageHandler;

    private WorkingThreadInvoker myWorker;

    private String myWorkingThreadName = "";
    
    private Object myLock = new Object();
    
    private String TracedObject()
    {
        return "WorkingThread '" + myWorkingThreadName + "' ";
    }
}
