package eneter.net.system;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;


public class EventImpl<T>
{
    public void subscribe(IMethod2<Object, T> eventHandler)
    {
        if (eventHandler == null)
        {
            throw new InvalidParameterException("The input parameter eventHandler is null.");
        }
        
        synchronized (mySubscribedEventHandlers)
        {
            // Store the event handler.
            mySubscribedEventHandlers.add(eventHandler);
        }
    }
    
    public void unsubscribe(IMethod2<Object, T> eventHandler)
    {
        if (eventHandler == null)
        {
            throw new InvalidParameterException("The input parameter eventHandler is null.");
        }
        
        synchronized (mySubscribedEventHandlers)
        {
            // Remove event handler if it is there. 
            mySubscribedEventHandlers.remove(eventHandler);
        }
    }
    
    public void update(Object sender, T eventArgs)
            throws Exception
    {
        ArrayList<IMethod2<Object, T>> aSafeCopyOfSubscribers;
        
        synchronized (mySubscribedEventHandlers)
        {
            aSafeCopyOfSubscribers = new ArrayList<IMethod2<Object,T>>(mySubscribedEventHandlers); 
        }
        
        // Use the safe copy to iterate via subscribed observers and notify them about the event.
        Iterator<IMethod2<Object, T>> it = aSafeCopyOfSubscribers.iterator();
        while (it.hasNext())
        {
            IMethod2<Object, T> anEventHandler = it.next();
            
            // Notify the subscriber.
            anEventHandler.invoke(sender, eventArgs);
        }
    }
    
    /*
     * Returns true if nobody is subscribed.
     */
    public boolean isEmpty()
    {
        synchronized (mySubscribedEventHandlers)
        {
            return mySubscribedEventHandlers.isEmpty();
        }
    }
    
    private ArrayList<IMethod2<Object, T>> mySubscribedEventHandlers = new ArrayList<IMethod2<Object, T>>();
}
