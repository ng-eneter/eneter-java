/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.threadpoolmessagingsystem;

import java.util.HashMap;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.IMessagingProvider;
import eneter.net.system.IMethod1;
import eneter.net.system.threading.ThreadPool;

class ThreadPoolMessagingProvider implements IMessagingProvider
{
    public void sendMessage(String receiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get the message handler
            final IMethod1<Object> aMessageHandler;

            synchronized (myRegisteredMessageHandlers)
            {
                aMessageHandler = myRegisteredMessageHandlers.get(receiverId);
            }

            // If the message handler was found then send the message
            if (aMessageHandler != null)
            {
                final Object aMsg = message;
                Runnable aHelperCallback = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            aMessageHandler.invoke(aMsg);
                        } catch (Exception e)
                        {
                            EneterTrace.error("ThreadPoolMessaging detected an error during sending of the message.", e);
                        }
                    }
                };
                
                ThreadPool.queueUserWorkItem(aHelperCallback);
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

    public void registerMessageHandler(String receiverId, IMethod1<Object> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myRegisteredMessageHandlers)
            {
                if (myRegisteredMessageHandlers.containsKey(receiverId))
                {
                    String aMessage = "The receiver '" + receiverId + "' is already registered.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myRegisteredMessageHandlers.put(receiverId, messageHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void unregisterMessageHandler(String receiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myRegisteredMessageHandlers)
            {
                myRegisteredMessageHandlers.remove(receiverId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private HashMap<String, IMethod1<Object>> myRegisteredMessageHandlers = new HashMap<String, IMethod1<Object>>();
}
