/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;


public class EventImpl<T>
{
    public void raise(Object sender, T eventArgs)
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
    
    public Event<T> getApi()
    {
        return myEventApi;
    }
    
    /*
     * Returns true if somebody is subscribed.
     */
    public boolean isSubscribed()
    {
        synchronized (mySubscribedEventHandlers)
        {
            return !mySubscribedEventHandlers.isEmpty();
        }
    }
    
    private void subscribeClient(EventHandler<T> eventHandler)
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
    
    private void unsubscribeClient(EventHandler<T> eventHandler)
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
    
    private ArrayList<EventHandler<T>> mySubscribedEventHandlers = new ArrayList<EventHandler<T>>();
    
    private Event<T> myEventApi = new Event<T>()
    {
        @Override
        public void subscribe(EventHandler<T> eventHandler)
        {
            subscribeClient(eventHandler);
        }

        @Override
        public void unsubscribe(EventHandler<T> eventHandler)
        {
            unsubscribeClient(eventHandler);
        }
    };
}
