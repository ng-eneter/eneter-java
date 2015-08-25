/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.threadmessagingsystem;

import java.util.HashMap;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.IMethod1;

class ThreadMessagingProvider implements IMessagingProvider
{

    @Override
    public void sendMessage(String receiverId, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get the thread handling the input channel.
            WorkingThread<Object> aWorkingThread = null;

            myRegisteredMessageHandlersLock.lock();
            try
            {
                aWorkingThread = myRegisteredMessageHandlers.get(receiverId);
            }
            finally
            {
                myRegisteredMessageHandlersLock.unlock();
            }

            if (aWorkingThread != null)
            {
                aWorkingThread.enqueueMessage(message);
            }
            else
            {
                String anError = "The receiver '" + receiverId + "' does not exist.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
        
    }

    @Override
    public void registerMessageHandler(String receiverId,
            IMethod1<Object> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myRegisteredMessageHandlersLock.lock();
            try
            {
                if (myRegisteredMessageHandlers.containsKey(receiverId))
                {
                    String aMessage = "The receiver '" + receiverId + "' is already registered.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                // Create and register the working thread for the registering input channel.
                WorkingThread<Object> aWorkingThread = new WorkingThread<Object>();
                aWorkingThread.registerMessageHandler(messageHandler);
                myRegisteredMessageHandlers.put(receiverId, aWorkingThread);
            }
            finally
            {
                myRegisteredMessageHandlersLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unregisterMessageHandler(String receiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            WorkingThread<Object> aWorkingThread = null;

            myRegisteredMessageHandlersLock.lock();
            try
            {
                aWorkingThread = myRegisteredMessageHandlers.get(receiverId);
                myRegisteredMessageHandlers.remove(receiverId);
            }
            finally
            {
                myRegisteredMessageHandlersLock.unlock();
            }
         
            if (aWorkingThread != null)
            {
                aWorkingThread.unregisterMessageHandler();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ThreadLock myRegisteredMessageHandlersLock = new ThreadLock(); 
    private HashMap<String, WorkingThread<Object>> myRegisteredMessageHandlers = new HashMap<String, WorkingThread<Object>>();
}
