/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */
 
package eneter.messaging.dataprocessing.messagequeueing;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.threading.dispatching.internal.SyncDispatcher;
import eneter.net.system.IMethod1;


/**
 * Thread with the message queue.
 * If a message is put to the queue, the thread removes it from the queue and calls a call-back
 * method to process it.
 *
 * @param <TMessage> type of the message processed by the thread
 */
public class WorkingThread<TMessage>
{
    /**
     * Registers the method handling messages from the queue and starts the thread reading messages from the queue.
     * @param messageHandler Callback called from the working thread to process the message
     */
    public void registerMessageHandler(IMethod1<TMessage> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myLock.lock();
            try
            {
                if (myMessageHandler != null)
                {
                    String aMessage = TracedObject() + "has already registered the message handler.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                myMessageHandler = messageHandler;
            }
            finally
            {
                myLock.unlock();
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
            myLock.lock();
            try
            {
                myMessageHandler = null;
            }
            finally
            {
                myLock.unlock();
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
    public void enqueueMessage(final TMessage message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myLock.lock();
            try
            {
                if (myMessageHandler == null)
                {
                    String aMessage = TracedObject() + "failed to enqueue the message because the message handler is not registered.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
    
                // Note: If the message handler is unregistered before the message handler is processed from the queue
                //       then myMessageHandler will be null and the exception will occur. Therefore we need to store it locally.
                final IMethod1<TMessage> aMessageHandler = myMessageHandler;
                myWorker.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            aMessageHandler.invoke(message);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                });
            }
            finally
            {
                myLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private SyncDispatcher myWorker = new SyncDispatcher();
    private IMethod1<TMessage> myMessageHandler;
    private ThreadLock myLock = new ThreadLock();
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}
