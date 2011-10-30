package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import java.util.HashMap;

import eneter.messaging.messagingsystems.simplemessagingsystembase.IMessagingProvider;
import eneter.net.system.IMethod1;

class SynchronousMessagingProvider implements IMessagingProvider
{
    public void sendMessage(String receiverId, Object message)
    {
        // Get the message handler.
        IMethod1<Object> aMessageHandler = null;
        
        synchronized (myRegisteredMessageHandlers)
        {
            aMessageHandler = myRegisteredMessageHandlers.get(receiverId);
        }
        
        // If the message handler was found then send the message
        if (aMessageHandler != null)
        {
            aMessageHandler.invoke(message);
        }
        else
        {
            // TODO: Trace error.
            //string anError = "The receiver '" + receiverId + "' does not exist.";
            //EneterTrace.Error(anError);
            throw new IllegalStateException("The receiver '" + receiverId + "' does not exist.");
        }
        
    }

    public void registerMessageHandler(String receiverId, IMethod1<Object> messageHandler)
    {
        synchronized (myRegisteredMessageHandlers)
        {
            if (myRegisteredMessageHandlers.containsKey(receiverId))
            {
                //string aMessage = "The receiver '" + receiverId + "' is already registered.";
                //EneterTrace.Error(aMessage);
                throw new IllegalStateException("The receiver '" + receiverId + "' is already registered.");
            }

            myRegisteredMessageHandlers.put(receiverId, messageHandler);
        }
    }

    public void unregisterMessageHandler(String receiverId)
    {
        synchronized (myRegisteredMessageHandlers)
        {
            myRegisteredMessageHandlers.remove(receiverId);
        }
    }

    
    private HashMap<String, IMethod1<Object>> myRegisteredMessageHandlers = new HashMap<String, IMethod1<Object>>();

}
