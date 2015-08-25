/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import java.util.HashMap;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IMessagingProvider;
import eneter.net.system.IMethod1;

class SynchronousMessagingProvider implements IMessagingProvider
{
    public void sendMessage(String receiverId, Object message)
            throws Exception
    {
        // Get the message handler.
        IMethod1<Object> aMessageHandler = null;
        
        myRegisteredMessageHandlersLock.lock();
        try
        {
            aMessageHandler = myRegisteredMessageHandlers.get(receiverId);
        }
        finally
        {
            myRegisteredMessageHandlersLock.unlock();
        }
        
        // If the message handler was found then send the message
        if (aMessageHandler != null)
        {
            aMessageHandler.invoke(message);
        }
        else
        {
            String anError = "The receiver '" + receiverId + "' does not exist.";
            EneterTrace.error(anError);
            throw new IllegalStateException("The receiver '" + receiverId + "' does not exist.");
        }
        
    }

    public void registerMessageHandler(String receiverId, IMethod1<Object> messageHandler)
    {
        myRegisteredMessageHandlersLock.lock();
        try
        {
            if (myRegisteredMessageHandlers.containsKey(receiverId))
            {
                //string aMessage = "The receiver '" + receiverId + "' is already registered.";
                //EneterTrace.Error(aMessage);
                throw new IllegalStateException("The receiver '" + receiverId + "' is already registered.");
            }

            myRegisteredMessageHandlers.put(receiverId, messageHandler);
        }
        finally
        {
            myRegisteredMessageHandlersLock.unlock();
        }
    }

    public void unregisterMessageHandler(String receiverId)
    {
        myRegisteredMessageHandlersLock.lock();
        try
        {
            myRegisteredMessageHandlers.remove(receiverId);
        }
        finally
        {
            myRegisteredMessageHandlersLock.unlock();
        }
    }

    private ThreadLock myRegisteredMessageHandlersLock = new ThreadLock();
    private HashMap<String, IMethod1<Object>> myRegisteredMessageHandlers = new HashMap<String, IMethod1<Object>>();

}
